package com.example.jobagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户注册请求")
public class RegisterDTO {

    @NotBlank(message = "不能为空")
    @Size(min = 4, max = 50, message = "长度必须在4到50个字符之间")
    @Schema(description = "用户名", example = "student01")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 64, message = "长度必须在6到64个字符之间")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Size(max = 50, message = "长度不能超过50个字符")
    @Schema(description = "昵称", example = "小王")
    private String nickname;

    @Email(message = "格式不正确")
    @Size(max = 100, message = "长度不能超过100个字符")
    @Schema(description = "邮箱", example = "student@example.com")
    private String email;
}
