package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("job_post")
@EqualsAndHashCode(callSuper = true)
public class JobPost extends BaseEntity {

    private Long userId;

    private String jdText;

    private String source;

    private String companyName;

    private String jobName;

    private String city;

    private String salary;

    private String sourceLink;
}
