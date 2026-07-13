package com.example.jobagent.security;

import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.exception.BusinessException;

public final class UserContext {

    private static final ThreadLocal<LoginUser> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        USER_HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        LoginUser loginUser = USER_HOLDER.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long getUserId() {
        return get().getUserId();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
