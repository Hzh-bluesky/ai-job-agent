package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "简历项目优化请求")
public class ResumeRewriteDTO {

    @NotNull(message = "不能为空")
    @Positive(message = "必须大于0")
    private Long resumeId;

    @NotNull(message = "不能为空")
    @Positive(message = "必须大于0")
    private Long jobPostId;

    @NotBlank(message = "不能为空")
    @Size(max = 10000, message = "长度不能超过10000个字符")
    private String projectExperience;
}
