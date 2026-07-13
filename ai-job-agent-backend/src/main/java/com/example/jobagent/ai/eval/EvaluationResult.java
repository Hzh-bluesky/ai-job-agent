package com.example.jobagent.ai.eval;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EvaluationResult {

    private Boolean passed;

    private Integer score;

    private List<String> missingFields;

    private List<String> issues;

    private String suggestion;
}
