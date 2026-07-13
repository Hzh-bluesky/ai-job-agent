package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "生成Boss直聘打招呼话术请求")
public class GreetingGenerateDTO {

    @NotNull(message = "不能为空")
    @Positive(message = "必须大于0")
    private Long resumeId;

    @NotNull(message = "不能为空")
    @Positive(message = "必须大于0")
    private Long jobPostId;
}
