package com.example.jobagent.ai;

import com.example.jobagent.config.LLMProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service("groqAIService")
@RequiredArgsConstructor
public class GroqAIService implements LLMService {

    private final LLMProperties llmProperties;
    private final ObjectMapper objectMapper;

    @Override
    public LLMResponse chat(LLMRequest request) {
        LLMProperties.Groq groq = llmProperties.getGroq();
        String model = StringUtils.hasText(groq.getModel()) ? groq.getModel() : "llama-3.1-8b-instant";
        Double temperature = groq.getTemperature() == null ? 0.2 : groq.getTemperature();
        Integer maxTokens = groq.getMaxTokens() == null ? 4096 : groq.getMaxTokens();

        if (!StringUtils.hasText(groq.getApiKey())) {
            return failed(model, "Groq API Key未配置，请设置GROQ_API_KEY", "CONFIG_ERROR");
        }

        try {
            String url = normalizeBaseUrl(groq.getBaseUrl()) + "/chat/completions";
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", temperature,
                    "max_completion_tokens", maxTokens,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", buildSystemPrompt(request.getSkillName())),
                            Map.of("role", "user", "content", request.getPrompt())
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groq.getApiKey());

            ResponseEntity<String> responseEntity = buildRestTemplate(groq.getTimeoutSeconds())
                    .postForEntity(url, new HttpEntity<>(body, headers), String.class);

            String responseBody = responseEntity.getBody();
            String content = extractContent(responseBody);
            if (!StringUtils.hasText(content)) {
                return LLMResponse.builder()
                        .provider("GROQ")
                        .model(model)
                        .success(false)
                        .statusCode(responseEntity.getStatusCode().value())
                        .failureType("EMPTY_RESPONSE")
                        .rawResponseBody(responseBody)
                        .errorMessage("Groq返回内容为空")
                        .build();
            }

            return LLMResponse.builder()
                    .provider("GROQ")
                    .model(model)
                    .success(true)
                    .statusCode(responseEntity.getStatusCode().value())
                    .content(content)
                    .rawResponseBody(responseBody)
                    .build();
        } catch (HttpStatusCodeException ex) {
            return LLMResponse.builder()
                    .provider("GROQ")
                    .model(model)
                    .success(false)
                    .statusCode(ex.getStatusCode().value())
                    .failureType(httpFailureType(ex.getStatusCode().value()))
                    .rawResponseBody(ex.getResponseBodyAsString())
                    .errorMessage("Groq调用失败：" + ex.getStatusCode())
                    .build();
        } catch (RestClientException ex) {
            return failed(model, "Groq网络调用失败：" + ex.getMessage(), clientFailureType(ex.getMessage()));
        } catch (Exception ex) {
            return failed(model, "Groq响应解析失败：" + ex.getMessage(), "RESPONSE_PARSE_ERROR");
        }
    }

    private RestTemplate buildRestTemplate(Integer timeoutSeconds) {
        int timeoutMillis = Math.max(1, timeoutSeconds == null ? 60 : timeoutSeconds) * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return new RestTemplate(factory);
    }

    private String buildSystemPrompt(String skillName) {
        return """
                你是AI求职投递助手Agent中的%s。
                你的输出必须是合法JSON字符串，不要输出Markdown代码块，不要输出解释性文字。
                如果字段没有明确答案，可以返回空字符串或合理的保守描述，不要伪造用户经历。
                """.formatted(skillName);
    }

    private String extractContent(String responseBody) throws Exception {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            return null;
        }
        return normalizeJsonContent(contentNode.asText());
    }

    private String normalizeJsonContent(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                text = text.substring(firstLineEnd + 1, lastFence).trim();
            }
        }

        int objectStart = text.indexOf('{');
        int arrayStart = text.indexOf('[');
        int start;
        if (objectStart < 0) {
            start = arrayStart;
        } else if (arrayStart < 0) {
            start = objectStart;
        } else {
            start = Math.min(objectStart, arrayStart);
        }

        int objectEnd = text.lastIndexOf('}');
        int arrayEnd = text.lastIndexOf(']');
        int end = Math.max(objectEnd, arrayEnd);
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }
        return text;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://api.groq.com/openai/v1";
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String httpFailureType(int statusCode) {
        if (statusCode == 401) {
            return "AUTH_ERROR";
        }
        if (statusCode == 402) {
            return "QUOTA_ERROR";
        }
        if (statusCode == 403) {
            return "FORBIDDEN";
        }
        if (statusCode == 429) {
            return "RATE_LIMIT";
        }
        if (statusCode >= 500) {
            return "SERVER_ERROR";
        }
        return "HTTP_ERROR";
    }

    private String clientFailureType(String message) {
        String text = message == null ? "" : message.toLowerCase();
        if (text.contains("timed out") || text.contains("timeout")) {
            return "TIMEOUT";
        }
        return "NETWORK_ERROR";
    }

    private LLMResponse failed(String model, String message, String failureType) {
        return LLMResponse.builder()
                .provider("GROQ")
                .model(model)
                .success(false)
                .failureType(failureType)
                .errorMessage(message)
                .build();
    }
}
