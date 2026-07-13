package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("application_record")
@EqualsAndHashCode(callSuper = true)
public class ApplicationRecord extends BaseEntity {

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
}
