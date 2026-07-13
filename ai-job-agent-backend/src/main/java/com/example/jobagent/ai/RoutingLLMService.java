package com.example.jobagent.ai;

import com.example.jobagent.ai.eval.AiOutputEvaluator;
import com.example.jobagent.ai.eval.EvaluationResult;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.config.LLMProperties;
import com.example.jobagent.entity.AiCallLog;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.service.AiCallLogService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Primary
@Service
public class RoutingLLMService implements LLMService {

    private static final int MAX_RETRY_COUNT = 2;
    private static final int MAX_ATTEMPT_COUNT = MAX_RETRY_COUNT + 1;

    private final LLMProperties llmProperties;
    private final LLMService mockAIService;
    private final LLMService deepSeekAIService;
    private final LLMService groqAIService;
    private final LLMService qwenAIService;
    private final LLMService fastApiAIService;
    private final AiCallLogService aiCallLogService;
    private final AiOutputEvaluator aiOutputEvaluator;

    public RoutingLLMService(LLMProperties llmProperties,
                             @Qualifier("mockAIService") LLMService mockAIService,
                             @Qualifier("deepSeekAIService") LLMService deepSeekAIService,
                             @Qualifier("groqAIService") LLMService groqAIService,
                             @Qualifier("qwenAIService") LLMService qwenAIService,
                             @Qualifier("fastApiAIService") LLMService fastApiAIService,
                             AiCallLogService aiCallLogService,
                             AiOutputEvaluator aiOutputEvaluator) {
        this.llmProperties = llmProperties;
        this.mockAIService = mockAIService;
        this.deepSeekAIService = deepSeekAIService;
        this.groqAIService = groqAIService;
        this.qwenAIService = qwenAIService;
        this.fastApiAIService = fastApiAIService;
        this.aiCallLogService = aiCallLogService;
        this.aiOutputEvaluator = aiOutputEvaluator;
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        String provider = llmProperties.normalizedProvider();
        ProviderRoute route = resolveRoute(provider);

        if ("MOCK".equals(route.getProvider())) {
            InvocationResult mockResult = invokeWithEvaluation(route, request, 1, false);
            if (mockResult.isPassed()) {
                return mockResult.getResponse();
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MockAIService输出质量检查失败：" + evaluationMessage(mockResult));
        }

        InvocationResult result = invokeWithRetry(route, request);
        if (result.isPassed()) {
            return result.getResponse();
        }

        if (llmProperties.isFallbackMock()) {
            log.warn("[LLM] fallback to mock provider={}, skillName={}, failureType={}, issue={}",
                    route.getProvider(), request.getSkillName(), result.getFailureType(), evaluationMessage(result));
            InvocationResult fallbackResult = invokeWithEvaluation(
                    new ProviderRoute(mockAIService, "MOCK", "mock-ai-service-v1"),
                    request,
                    result.getAttemptCount() + 1,
                    true
            );
            if (fallbackResult.isPassed()) {
                return fallbackResult.getResponse();
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "大模型与Mock兜底均失败：" + evaluationMessage(fallbackResult));
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "大模型调用失败：" + evaluationMessage(result));
    }

    private InvocationResult invokeWithRetry(ProviderRoute route, LLMRequest request) {
        InvocationResult lastResult = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPT_COUNT; attempt++) {
            LLMRequest attemptRequest = buildAttemptRequest(request, attempt, lastResult);
            InvocationResult current = invokeWithEvaluation(route, attemptRequest, attempt, false);
            lastResult = current;
            if (current.isPassed()) {
                return current;
            }

            boolean retryable = attempt < MAX_ATTEMPT_COUNT && shouldRetry(current);
            log.warn("[LLM] attempt result provider={}, model={}, skillName={}, attempt={}, passed={}, score={}, failureType={}, retry={}, issue={}",
                    route.getProvider(),
                    route.getModel(),
                    request.getSkillName(),
                    attempt,
                    current.isPassed(),
                    current.getEvaluationResult() == null ? null : current.getEvaluationResult().getScore(),
                    current.getFailureType(),
                    retryable,
                    evaluationMessage(current));

            if (!retryable) {
                return current;
            }
            sleepBeforeRetry(attempt);
        }
        return lastResult;
    }

    private InvocationResult invokeWithEvaluation(ProviderRoute route,
                                                 LLMRequest request,
                                                 int attempt,
                                                 boolean fallbackUsed) {
        long startTime = System.currentTimeMillis();
        LLMResponse response;
        try {
            response = route.getDelegate().chat(request);
        } catch (Exception ex) {
            response = LLMResponse.builder()
                    .provider(route.getProvider())
                    .model(route.getModel())
                    .success(false)
                    .failureType("CLIENT_EXCEPTION")
                    .errorMessage(ex.getMessage())
                    .build();
        }
        if (response == null) {
            response = LLMResponse.builder()
                    .provider(route.getProvider())
                    .model(route.getModel())
                    .success(false)
                    .failureType("EMPTY_RESPONSE")
                    .errorMessage("LLMResponse为空")
                    .build();
        }

        long latencyMs = System.currentTimeMillis() - startTime;
        EvaluationResult evaluationResult = aiOutputEvaluator.evaluate(request, response);
        String failureType = determineFailureType(response, evaluationResult);
        boolean passed = isSuccess(response) && Boolean.TRUE.equals(evaluationResult.getPassed());
        if (!passed) {
            response.setSuccess(false);
            response.setFailureType(failureType);
            response.setErrorMessage(defaultIfBlank(response.getErrorMessage(), evaluationResult.getSuggestion()));
        }

        saveLogSafely(request, route, response, evaluationResult, failureType, latencyMs, attempt, fallbackUsed, passed);
        log.info("[LLM] call logged provider={}, model={}, skillName={}, attempt={}, fallback={}, passed={}, score={}, latencyMs={}",
                route.getProvider(),
                route.getModel(),
                request.getSkillName(),
                attempt,
                fallbackUsed,
                passed,
                evaluationResult.getScore(),
                latencyMs);

        return new InvocationResult(response, evaluationResult, attempt, failureType, passed);
    }

    private LLMRequest buildAttemptRequest(LLMRequest original, int attempt, InvocationResult previousResult) {
        if (attempt <= 1 || previousResult == null) {
            return original;
        }
        String repairPrompt = original.getPrompt() + """

                [上一次AI输出质量检查未通过，请修复后重新输出]
                缺失字段：%s
                发现问题：%s
                修复建议：%s
                要求：只返回合法JSON；字段名必须和原要求完全一致；不要输出Markdown代码块；不要输出解释文字。
                """.formatted(
                missingFieldsText(previousResult.getEvaluationResult()),
                issuesText(previousResult),
                evaluationMessage(previousResult)
        );

        return LLMRequest.builder()
                .userId(original.getUserId())
                .agentName(original.getAgentName())
                .skillName(original.getSkillName())
                .prompt(repairPrompt)
                .variables(original.getVariables())
                .build();
    }

    private boolean shouldRetry(InvocationResult result) {
        String failureType = result.getFailureType();
        if ("AUTH_ERROR".equals(failureType)
                || "QUOTA_ERROR".equals(failureType)
                || "FORBIDDEN".equals(failureType)
                || "CONFIG_ERROR".equals(failureType)) {
            return false;
        }
        return "TIMEOUT".equals(failureType)
                || "RATE_LIMIT".equals(failureType)
                || "SERVER_ERROR".equals(failureType)
                || "NETWORK_ERROR".equals(failureType)
                || "EMPTY_RESPONSE".equals(failureType)
                || "RESPONSE_PARSE_ERROR".equals(failureType)
                || "JSON_PARSE_FAILED".equals(failureType)
                || "MISSING_FIELDS".equals(failureType)
                || "LOW_QUALITY".equals(failureType)
                || "EVALUATION_FAILED".equals(failureType)
                || "CLIENT_EXCEPTION".equals(failureType);
    }

    private String determineFailureType(LLMResponse response, EvaluationResult evaluationResult) {
        if (response != null && StringUtils.hasText(response.getFailureType())) {
            return response.getFailureType();
        }
        if (evaluationResult == null) {
            return "EVALUATION_FAILED";
        }
        if (Boolean.TRUE.equals(evaluationResult.getPassed())) {
            return null;
        }
        boolean jsonParseFailed = evaluationResult.getIssues() != null
                && evaluationResult.getIssues().stream().anyMatch(issue -> issue.toLowerCase().contains("json parse"));
        if (jsonParseFailed) {
            return "JSON_PARSE_FAILED";
        }
        if (evaluationResult.getMissingFields() != null && !evaluationResult.getMissingFields().isEmpty()) {
            return "MISSING_FIELDS";
        }
        if (evaluationResult.getScore() != null && evaluationResult.getScore() < 70) {
            return "LOW_QUALITY";
        }
        return "EVALUATION_FAILED";
    }

    private ProviderRoute resolveRoute(String provider) {
        if ("mock".equals(provider)) {
            return new ProviderRoute(mockAIService, "MOCK", "mock-ai-service-v1");
        }
        if ("deepseek".equals(provider)) {
            return new ProviderRoute(deepSeekAIService, "DEEPSEEK", llmProperties.getDeepseek().getModel());
        }
        if ("groq".equals(provider)) {
            return new ProviderRoute(groqAIService, "GROQ", llmProperties.getGroq().getModel());
        }
        if ("qwen".equals(provider)) {
            return new ProviderRoute(qwenAIService, "QWEN", llmProperties.getQwen().getModel());
        }
        if ("fastapi".equals(provider)) {
            return new ProviderRoute(fastApiAIService, "FASTAPI", llmProperties.getFastapi().getModel());
        }
        if (llmProperties.isFallbackMock()) {
            return new ProviderRoute(mockAIService, "MOCK", "mock-ai-service-v1");
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的LLM provider：" + provider);
    }

    private void saveLogSafely(LLMRequest request,
                               ProviderRoute route,
                               LLMResponse response,
                               EvaluationResult evaluationResult,
                               String failureType,
                               long latencyMs,
                               int attempt,
                               boolean fallbackUsed,
                               boolean passed) {
        try {
            AiCallLog logRecord = new AiCallLog();
            logRecord.setUserId(request.getUserId());
            logRecord.setAgentName(request.getAgentName());
            logRecord.setSkillName(request.getSkillName());
            logRecord.setProvider(route.getProvider());
            logRecord.setModel(route.getModel());
            logRecord.setPrompt(request.getPrompt());
            logRecord.setResponseBody(responseBody(response));
            logRecord.setSuccess(passed ? 1 : 0);
            logRecord.setErrorMessage(errorMessage(response));
            logRecord.setLatencyMs(latencyMs);
            logRecord.setAttemptCount(attempt);
            logRecord.setQualityScore(evaluationResult == null ? null : evaluationResult.getScore());
            logRecord.setEvaluationPassed(evaluationResult != null && Boolean.TRUE.equals(evaluationResult.getPassed()) ? 1 : 0);
            logRecord.setEvaluationIssues(evaluationResult == null ? null : evaluationMessage(evaluationResult));
            logRecord.setFailureType(failureType);
            logRecord.setFallbackUsed(fallbackUsed ? 1 : 0);
            aiCallLogService.saveLog(logRecord);
        } catch (Exception ex) {
            log.warn("AI调用日志写入失败", ex);
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(attempt * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isSuccess(LLMResponse response) {
        return response != null && Boolean.TRUE.equals(response.getSuccess());
    }

    private String responseBody(LLMResponse response) {
        if (response == null) {
            return null;
        }
        return response.getRawResponseBody() != null ? response.getRawResponseBody() : response.getContent();
    }

    private String errorMessage(LLMResponse response) {
        if (response == null) {
            return "LLMResponse为空";
        }
        return response.getErrorMessage();
    }

    private String evaluationMessage(InvocationResult result) {
        if (result == null) {
            return "无评估结果";
        }
        if (result.getEvaluationResult() != null) {
            return evaluationMessage(result.getEvaluationResult());
        }
        return defaultIfBlank(errorMessage(result.getResponse()), result.getFailureType());
    }

    private String evaluationMessage(EvaluationResult result) {
        if (result == null) {
            return null;
        }
        String missingFields = result.getMissingFields() == null || result.getMissingFields().isEmpty()
                ? ""
                : "missingFields=" + String.join(",", result.getMissingFields());
        String issues = result.getIssues() == null || result.getIssues().isEmpty()
                ? ""
                : "issues=" + String.join("；", result.getIssues());
        String suggestion = defaultIfBlank(result.getSuggestion(), "");
        return (missingFields + " " + issues + " suggestion=" + suggestion).trim();
    }

    private String missingFieldsText(EvaluationResult result) {
        if (result == null || result.getMissingFields() == null || result.getMissingFields().isEmpty()) {
            return "";
        }
        return String.join(",", result.getMissingFields());
    }

    private String issuesText(InvocationResult result) {
        if (result == null) {
            return "";
        }
        EvaluationResult evaluationResult = result.getEvaluationResult();
        if (evaluationResult == null || evaluationResult.getIssues() == null || evaluationResult.getIssues().isEmpty()) {
            return defaultIfBlank(result.getFailureType(), "");
        }
        return String.join("；", evaluationResult.getIssues());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    @Getter
    private static class ProviderRoute {
        private final LLMService delegate;
        private final String provider;
        private final String model;

        private ProviderRoute(LLMService delegate, String provider, String model) {
            this.delegate = delegate;
            this.provider = provider;
            this.model = model;
        }
    }

    @Getter
    private static class InvocationResult {
        private final LLMResponse response;
        private final EvaluationResult evaluationResult;
        private final int attemptCount;
        private final String failureType;
        private final boolean passed;

        private InvocationResult(LLMResponse response,
                                 EvaluationResult evaluationResult,
                                 int attemptCount,
                                 String failureType,
                                 boolean passed) {
            this.response = response;
            this.evaluationResult = evaluationResult;
            this.attemptCount = attemptCount;
            this.failureType = failureType;
            this.passed = passed;
        }
    }
}
