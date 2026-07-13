package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录成功响应")
public class LoginVO {

    @Schema(description = "JWT token")
    private String token;

    @Schema(description = "用户基本信息")
    private UserProfileVO user;
}
