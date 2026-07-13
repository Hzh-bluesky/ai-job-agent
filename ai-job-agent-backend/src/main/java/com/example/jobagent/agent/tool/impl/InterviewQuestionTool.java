package com.example.jobagent.agent.tool.impl;

import com.example.jobagent.agent.tool.AgentTool;
import com.example.jobagent.agent.tool.ToolContext;
import com.example.jobagent.agent.tool.ToolResult;
import com.example.jobagent.dto.InterviewGenerateDTO;
import com.example.jobagent.service.InterviewService;
import com.example.jobagent.vo.InterviewQuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewQuestionTool implements AgentTool {

    public static final String NAME = "interview_question";

    private final InterviewService interviewService;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Generate interview questions and answer ideas";
    }

    @Override
    public ToolResult<InterviewQuestionVO> execute(ToolContext context) {
        try {
            InterviewGenerateDTO dto = new InterviewGenerateDTO();
            dto.setJobPostId(context.getJobPostId());

            InterviewQuestionVO interview = interviewService.generate(context.getUserId(), dto);
            context.setInterviewQuestions(interview);
            context.setInterviewRecordId(interview.getId());
            log.info("[AgentTool] success toolName={}, userId={}, resumeId={}, jobPostId={}, interviewRecordId={}",
                    getName(), context.getUserId(), context.getResumeId(), context.getJobPostId(), interview.getId());
            return ToolResult.success(getName(), interview);
        } catch (Exception e) {
            log.error("[AgentTool] failed toolName={}, userId={}, resumeId={}, jobPostId={}",
                    getName(), context.getUserId(), context.getResumeId(), context.getJobPostId(), e);
            return ToolResult.failure(getName(), "Interview question generation failed: " + e.getMessage());
        }
    }
}
