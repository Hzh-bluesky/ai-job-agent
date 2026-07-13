package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "当前登录用户信息")
public class UserProfileVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String role;

    private Integer status;

    private LocalDateTime createTime;
}
