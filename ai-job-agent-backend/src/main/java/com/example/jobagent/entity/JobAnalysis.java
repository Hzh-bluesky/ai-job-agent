package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("job_analysis")
@EqualsAndHashCode(callSuper = true)
public class JobAnalysis extends BaseEntity {

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
}
