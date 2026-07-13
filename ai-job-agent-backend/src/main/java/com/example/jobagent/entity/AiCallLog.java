package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("ai_call_log")
@EqualsAndHashCode(callSuper = true)
public class AiCallLog extends BaseEntity {

    private Long userId;

    private String agentName;

    private String skillName;

    private String provider;

    private String model;

    private String prompt;

    private String responseBody;

    private Integer success;

    private String errorMessage;

    private Long latencyMs;

    private Integer attemptCount;

    private Integer qualityScore;

    private Integer evaluationPassed;

    private String evaluationIssues;

    private String failureType;

    private Integer fallbackUsed;
}
