package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "匹配报告分页查询请求")
public class MatchPageQueryDTO {

    @Min(value = 1, message = "必须大于等于1")
    private Long pageNo = 1L;

    @Min(value = 1, message = "必须大于等于1")
    @Max(value = 100, message = "不能超过100")
    private Long pageSize = 10L;
}
