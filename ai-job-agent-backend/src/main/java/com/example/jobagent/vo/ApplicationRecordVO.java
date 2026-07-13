package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "投递记录响应")
public class ApplicationRecordVO {

    private Long id;

    private Long userId;

    private Long resumeId;

    private Long jobPostId;

    private Long matchReportId;

    private String companyName;

    private String jobName;

    private String city;

    private String salary;

    private String jdText;

    private Integer matchScore;

    private String status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
