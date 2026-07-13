package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改投递状态请求")
public class ApplicationStatusUpdateDTO {

    @NotBlank(message = "不能为空")
    @Size(max = 30, message = "长度不能超过30个字符")
    private String status;
}
