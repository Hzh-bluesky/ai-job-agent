package com.example.jobagent.ai;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class LLMRequest {

    private Long userId;

    private String agentName;

    private String skillName;

    private String prompt;

    private Map<String, Object> variables;
}
