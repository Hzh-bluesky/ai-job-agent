package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Apply plan page query")
public class ApplyPlanPageQueryDTO {

    @Min(value = 1, message = "pageNo must be greater than or equal to 1")
    private Long pageNo = 1L;

    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    @Max(value = 100, message = "pageSize cannot exceed 100")
    private Long pageSize = 10L;
}
