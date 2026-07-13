package com.example.jobagent.agent.tool;

public interface AgentTool {

    String getName();

    String getDescription();

    ToolResult<?> execute(ToolContext context);
}
