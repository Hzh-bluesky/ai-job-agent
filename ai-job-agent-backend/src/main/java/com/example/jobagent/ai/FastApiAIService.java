package com.example.jobagent.ai;

import com.example.jobagent.config.LLMProperties;
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

import java.util.HashMap;
import java.util.Map;

@Service("fastApiAIService")
@RequiredArgsConstructor
public class FastApiAIService implements LLMService {

    private final LLMProperties llmProperties;

    @Override
    public LLMResponse chat(LLMRequest request) {
        LLMProperties.Fastapi fastapi = llmProperties.getFastapi();
        String model = StringUtils.hasText(fastapi.getModel()) ? fastapi.getModel() : "qwen-plus";
        String provider = StringUtils.hasText(fastapi.getProvider()) ? fastapi.getProvider() : "qwen";

        try {
            String url = normalizeBaseUrl(fastapi.getBaseUrl()) + "/api/ai/chat";
            Map<String, Object> body = new HashMap<>();
            body.put("provider", provider);
            body.put("model", model);
            body.put("skillName", request.getSkillName());
            body.put("prompt", request.getPrompt());
            body.put("ragContext", request.getVariables() == null ? null : request.getVariables().get("ragContext"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<FastApiChatResponse> responseEntity = buildRestTemplate(fastapi.getTimeoutSeconds())
                    .postForEntity(url, new HttpEntity<>(body, headers), FastApiChatResponse.class);

            FastApiChatResponse response = responseEntity.getBody();
            if (response == null) {
                return failed(model, "FastAPI AI Service返回为空", "EMPTY_RESPONSE");
            }
            return LLMResponse.builder()
                    .provider("FASTAPI")
                    .model(response.model() == null ? model : response.model())
                    .content(response.content())
                    .rawResponseBody(response.content())
                    .success(Boolean.TRUE.equals(response.success()))
                    .errorMessage(response.errorMessage())
                    .statusCode(responseEntity.getStatusCode().value())
                    .failureType(Boolean.TRUE.equals(response.success()) ? null : remoteFailureType(response.errorMessage()))
                    .build();
        } catch (HttpStatusCodeException ex) {
            return LLMResponse.builder()
                    .provider("FASTAPI")
                    .model(model)
                    .success(false)
                    .statusCode(ex.getStatusCode().value())
                    .failureType(httpFailureType(ex.getStatusCode().value()))
                    .rawResponseBody(ex.getResponseBodyAsString())
                    .errorMessage("FastAPI AI Service调用失败：" + ex.getStatusCode())
                    .build();
        } catch (RestClientException ex) {
            return failed(model, "FastAPI AI Service网络调用失败：" + ex.getMessage(), clientFailureType(ex.getMessage()));
        } catch (Exception ex) {
            return failed(model, "FastAPI AI Service响应解析失败：" + ex.getMessage(), "RESPONSE_PARSE_ERROR");
        }
    }

    private RestTemplate buildRestTemplate(Integer timeoutSeconds) {
        int timeoutMillis = Math.max(1, timeoutSeconds == null ? 90 : timeoutSeconds) * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return new RestTemplate(factory);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "http://localhost:8000";
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

    private String remoteFailureType(String message) {
        String text = message == null ? "" : message.toLowerCase();
        if (text.contains("dashscope_api_key") || text.contains("unsupported provider")) {
            return "CONFIG_ERROR";
        }
        if (text.contains("http 401")) {
            return "AUTH_ERROR";
        }
        if (text.contains("http 402")) {
            return "QUOTA_ERROR";
        }
        if (text.contains("http 403")) {
            return "FORBIDDEN";
        }
        if (text.contains("http 429")) {
            return "RATE_LIMIT";
        }
        if (text.contains("http 500") || text.contains("http 502") || text.contains("http 503") || text.contains("http 504")) {
            return "SERVER_ERROR";
        }
        if (text.contains("timed out") || text.contains("timeout")) {
            return "TIMEOUT";
        }
        return "REMOTE_AI_SERVICE_ERROR";
    }

    private LLMResponse failed(String model, String message, String failureType) {
        return LLMResponse.builder()
                .provider("FASTAPI")
                .model(model)
                .success(false)
                .failureType(failureType)
                .errorMessage(message)
                .build();
    }

    private record FastApiChatResponse(
            String content,
            Boolean success,
            Long latencyMs,
            String provider,
            String model,
            String errorMessage
    ) {
    }
}
