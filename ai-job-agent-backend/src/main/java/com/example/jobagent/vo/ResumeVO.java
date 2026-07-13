package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "简历响应")
public class ResumeVO {

    private Long id;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
