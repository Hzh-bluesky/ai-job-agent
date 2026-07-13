package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resume")
@EqualsAndHashCode(callSuper = true)
public class Resume extends BaseEntity {

    private Long userId;

    private String title;

    private String name;

    private String school;

    private String major;

    private String grade;

    private String techStack;

    private String projectExperience;

    private String internshipExperience;

    private String selfIntroduction;

    private Integer isDefault;
}
