package com.example.jobagent.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success", HttpStatus.OK),
    PARAM_ERROR(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "请先登录", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "没有访问权限", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
    USERNAME_EXISTS(40900, "用户名已存在", HttpStatus.CONFLICT),
    LOGIN_FAILED(40101, "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    USER_DISABLED(40301, "账号已被禁用", HttpStatus.FORBIDDEN),
    SYSTEM_ERROR(50000, "系统内部异常", HttpStatus.INTERNAL_SERVER_ERROR);

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(Integer code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
