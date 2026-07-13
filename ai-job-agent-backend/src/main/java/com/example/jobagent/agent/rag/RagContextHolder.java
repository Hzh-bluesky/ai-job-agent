package com.example.jobagent.agent.rag;

import org.springframework.util.StringUtils;

public final class RagContextHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private RagContextHolder() {
    }

    public static void set(String ragContext) {
        if (StringUtils.hasText(ragContext)) {
            HOLDER.set(ragContext);
        }
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
