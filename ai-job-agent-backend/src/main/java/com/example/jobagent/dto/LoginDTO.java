package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户登录请求")
public class LoginDTO {

    @NotBlank(message = "不能为空")
    @Schema(description = "用户名", example = "student01")
    private String username;

    @NotBlank(message = "不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;
}
