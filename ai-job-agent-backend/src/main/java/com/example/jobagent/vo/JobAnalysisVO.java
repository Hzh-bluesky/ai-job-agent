package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "岗位分析结果")
public class JobAnalysisVO {

    private Long id;

    private Long userId;

    private Long jobPostId;

    private String companyName;

    private String jobName;

    private String city;

    private String salary;

    private String education;

    private String internshipCycle;

    private String techStack;

    private String responsibilities;

    private String requirements;

    private String bonusPoints;

    private String riskPoints;

    private String rawResult;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
