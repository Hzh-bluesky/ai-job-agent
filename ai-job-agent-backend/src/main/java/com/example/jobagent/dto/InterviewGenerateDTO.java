package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "生成面试题请求")
public class InterviewGenerateDTO {

    @NotNull(message = "不能为空")
    @Positive(message = "必须大于0")
    private Long jobPostId;
}
