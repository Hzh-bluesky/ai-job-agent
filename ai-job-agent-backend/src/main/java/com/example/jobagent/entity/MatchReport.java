package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("match_report")
@EqualsAndHashCode(callSuper = true)
public class MatchReport extends BaseEntity {

    private Long userId;

    private Long resumeId;

    private Long jobPostId;

    private Long jobAnalysisId;

    private Integer overallScore;

    private Integer techScore;

    private Integer projectScore;

    private Integer educationScore;

    private String advantageAnalysis;

    private String weaknessAnalysis;

    private String suggestion;

    private Integer isRecommended;

    private String rawResult;
}
