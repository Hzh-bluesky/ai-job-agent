package com.example.jobagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LLMProperties {

    private String provider = "mock";

    private Boolean fallbackMock = true;

    private DeepSeek deepseek = new DeepSeek();

    private Groq groq = new Groq();

    private Qwen qwen = new Qwen();

    private Fastapi fastapi = new Fastapi();

    public String normalizedProvider() {
        return provider == null ? "mock" : provider.trim().toLowerCase();
    }

    public boolean isFallbackMock() {
        return Boolean.TRUE.equals(fallbackMock);
    }

    @Data
    public static class DeepSeek {

        private String apiKey;

        private String baseUrl = "https://api.deepseek.com";

        private String model = "deepseek-chat";

        private Double temperature = 0.2;

        private Integer maxTokens = 8192;

        private Integer timeoutSeconds = 60;
    }

    @Data
    public static class Groq {

        private String apiKey;

        private String baseUrl = "https://api.groq.com/openai/v1";

        private String model = "llama-3.1-8b-instant";

        private Double temperature = 0.2;

        private Integer maxTokens = 4096;

        private Integer timeoutSeconds = 60;
    }

    @Data
    public static class Qwen {

        private String apiKey;

        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

        private String model = "qwen-plus";

        private Double temperature = 0.2;

        private Integer maxTokens = 4096;

        private Integer timeoutSeconds = 60;
    }

    @Data
    public static class Fastapi {

        private String baseUrl = "http://localhost:8000";

        private String model = "qwen-plus";

        private String provider = "qwen";

        private Integer timeoutSeconds = 90;
    }
}
