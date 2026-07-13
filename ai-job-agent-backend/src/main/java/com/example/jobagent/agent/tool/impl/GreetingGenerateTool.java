package com.example.jobagent.agent.tool.impl;

import com.example.jobagent.agent.tool.AgentTool;
import com.example.jobagent.agent.tool.ToolContext;
import com.example.jobagent.agent.tool.ToolResult;
import com.example.jobagent.dto.GreetingGenerateDTO;
import com.example.jobagent.service.GreetingService;
import com.example.jobagent.vo.GreetingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GreetingGenerateTool implements AgentTool {

    public static final String NAME = "greeting_generate";

    private final GreetingService greetingService;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Generate Boss greeting text";
    }

    @Override
    public ToolResult<GreetingVO> execute(ToolContext context) {
        try {
            GreetingGenerateDTO dto = new GreetingGenerateDTO();
            dto.setResumeId(context.getResumeId());
            dto.setJobPostId(context.getJobPostId());

            GreetingVO greeting = greetingService.generate(context.getUserId(), dto);
            context.setGreeting(greeting);
            context.setGreetingRecordId(greeting.getId());
            log.info("[AgentTool] success toolName={}, userId={}, resumeId={}, jobPostId={}, greetingRecordId={}",
                    getName(), context.getUserId(), context.getResumeId(), context.getJobPostId(), greeting.getId());
            return ToolResult.success(getName(), greeting);
        } catch (Exception e) {
            log.error("[AgentTool] failed toolName={}, userId={}, resumeId={}, jobPostId={}",
                    getName(), context.getUserId(), context.getResumeId(), context.getJobPostId(), e);
            return ToolResult.failure(getName(), "Greeting generation failed: " + e.getMessage());
        }
    }
}
