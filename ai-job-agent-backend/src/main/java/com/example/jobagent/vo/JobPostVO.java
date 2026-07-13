package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "岗位信息")
public class JobPostVO {

    private Long id;

    private Long userId;

    private String jdText;

    private String source;

    private String sourceLink;

    private String companyName;

    private String jobName;

    private String city;

    private String salary;

    private JobAnalysisVO analysis;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
