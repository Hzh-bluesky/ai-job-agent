package com.example.jobagent.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobImportItemVO {

    private Integer rowNumber;

    private Boolean success;

    private String failureReason;

    private Long jobPostId;

    private Long jobAnalysisId;

    private String companyName;

    private String jobName;

    private String city;

    private String salary;

    private String sourceLink;
}
