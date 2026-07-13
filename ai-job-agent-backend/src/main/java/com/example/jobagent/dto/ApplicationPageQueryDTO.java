package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "投递记录分页查询请求")
public class ApplicationPageQueryDTO {

    @Min(value = 1, message = "必须大于等于1")
    private Long pageNo = 1L;

    @Min(value = 1, message = "必须大于等于1")
    @Max(value = 100, message = "不能超过100")
    private Long pageSize = 10L;
}
