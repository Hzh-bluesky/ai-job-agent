package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "岗位JD分析请求")
public class JobAnalyzeDTO {

    @NotBlank(message = "不能为空")
    @Size(max = 30000, message = "长度不能超过30000个字符")
    @Schema(description = "岗位JD原文")
    private String jdText;

    @Size(max = 50, message = "长度不能超过50个字符")
    @Schema(description = "岗位来源", example = "Boss直聘")
    private String source;
}
