package com.example.jobagent.enums;

import java.util.Arrays;

public enum ApplicationStatus {

    NOT_APPLIED("未投递"),
    APPLIED("已投递"),
    COMMUNICATING("待沟通"),
    INTERVIEWING("面试中"),
    REJECTED("已拒绝"),
    PASSED("已通过");

    private final String description;

    ApplicationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValid(String status) {
        if (status == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(item -> item.name().equals(status));
    }
}
