package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改投递记录请求")
public class ApplicationUpdateDTO {

    @Positive(message = "必须大于0")
    private Long resumeId;

    @Positive(message = "必须大于0")
    private Long jobPostId;

    @Positive(message = "必须大于0")
    private Long matchReportId;

    @Size(max = 100, message = "长度不能超过100个字符")
    private String companyName;

    @Size(max = 100, message = "长度不能超过100个字符")
    private String jobName;

    @Size(max = 50, message = "长度不能超过50个字符")
    private String city;

    @Size(max = 50, message = "长度不能超过50个字符")
    private String salary;

    @Size(max = 30000, message = "长度不能超过30000个字符")
    private String jdText;

    @Min(value = 0, message = "必须大于等于0")
    @Max(value = 100, message = "不能超过100")
    private Integer matchScore;

    @Size(max = 3000, message = "长度不能超过3000个字符")
    private String remark;
}
