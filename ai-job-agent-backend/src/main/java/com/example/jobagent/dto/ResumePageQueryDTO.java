package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "简历分页查询请求")
public class ResumePageQueryDTO {

    @Min(value = 1, message = "必须大于等于1")
    private Long pageNo = 1L;

    @Min(value = 1, message = "必须大于等于1")
    @Max(value = 100, message = "不能超过100")
    private Long pageSize = 10L;

    @Size(max = 100, message = "长度不能超过100个字符")
    private String keyword;
}
