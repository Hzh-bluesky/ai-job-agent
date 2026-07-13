package com.example.jobagent.agent.tool;

import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> toolMap;

    public ToolRegistry(List<AgentTool> tools) {
        Map<String, AgentTool> registry = new LinkedHashMap<>();
        for (AgentTool tool : tools) {
            AgentTool existingTool = registry.putIfAbsent(tool.getName(), tool);
            if (existingTool != null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Duplicate agent tool name: " + tool.getName());
            }
            log.info("[ToolRegistry] registered tool name={}, description={}", tool.getName(), tool.getDescription());
        }
        this.toolMap = Collections.unmodifiableMap(registry);
    }

    public AgentTool getTool(String name) {
        AgentTool tool = toolMap.get(name);
        if (tool == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Agent tool not found: " + name);
        }
        return tool;
    }

    public Map<String, AgentTool> getAllTools() {
        return toolMap;
    }
}
