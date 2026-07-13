package com.example.jobagent.controller;

import com.example.jobagent.common.Result;
import com.example.jobagent.security.UserContext;
import com.example.jobagent.service.AuthService;
import com.example.jobagent.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "当前用户")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        Long currentUserId = UserContext.getUserId();
        return Result.success(authService.getCurrentUser(currentUserId));
    }
}
