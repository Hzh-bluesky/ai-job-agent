package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "One click apply plan request")
public class ApplyPlanCreateDTO {

    @NotNull(message = "resumeId cannot be null")
    @Positive(message = "resumeId must be positive")
    private Long resumeId;

    @NotNull(message = "jobPostId cannot be null")
    @Positive(message = "jobPostId must be positive")
    private Long jobPostId;

    @Schema(description = "Whether to force regenerate a new apply plan")
    private Boolean forceRegenerate = false;
}
