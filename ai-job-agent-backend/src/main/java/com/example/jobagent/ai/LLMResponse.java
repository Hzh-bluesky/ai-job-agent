package com.example.jobagent.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMResponse {

    private String provider;

    private String model;

    private String content;

    private String rawResponseBody;

    private Boolean success;

    private String errorMessage;

    private Integer statusCode;

    private String failureType;
}
