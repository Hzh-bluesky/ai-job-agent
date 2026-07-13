package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "简历岗位匹配报告")
public class MatchReportVO {

    private Long id;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
