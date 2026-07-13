package com.example.jobagent.agent.tool;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolResult<T> {

    private boolean success;

    private String toolName;

    private T data;

    private String errorMessage;

    public static <T> ToolResult<T> success(String toolName, T data) {
        return ToolResult.<T>builder()
                .success(true)
                .toolName(toolName)
                .data(data)
                .build();
    }

    public static <T> ToolResult<T> failure(String toolName, String errorMessage) {
        return ToolResult.<T>builder()
                .success(false)
                .toolName(toolName)
                .errorMessage(errorMessage)
                .build();
    }
}
