package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新增简历请求")
public class ResumeCreateDTO {

    @NotBlank(message = "不能为空")
    @Size(max = 100, message = "长度不能超过100个字符")
    private String title;

    @Size(max = 50, message = "长度不能超过50个字符")
    private String name;

    @Size(max = 100, message = "长度不能超过100个字符")
    private String school;

    @Size(max = 100, message = "长度不能超过100个字符")
    private String major;

    @Size(max = 50, message = "长度不能超过50个字符")
    private String grade;

    @Size(max = 3000, message = "长度不能超过3000个字符")
    private String techStack;

    @Size(max = 10000, message = "长度不能超过10000个字符")
    private String projectExperience;

    @Size(max = 10000, message = "长度不能超过10000个字符")
    private String internshipExperience;

    @Size(max = 3000, message = "长度不能超过3000个字符")
    private String selfIntroduction;

    @Min(value = 0, message = "只能是0或1")
    @Max(value = 1, message = "只能是0或1")
    private Integer isDefault;
}
