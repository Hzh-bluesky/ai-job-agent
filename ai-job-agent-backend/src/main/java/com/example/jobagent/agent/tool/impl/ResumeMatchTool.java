package com.example.jobagent.agent.tool.impl;

import com.example.jobagent.agent.tool.AgentTool;
import com.example.jobagent.agent.tool.ToolContext;
import com.example.jobagent.agent.tool.ToolResult;
import com.example.jobagent.dto.MatchCreateDTO;
import com.example.jobagent.service.MatchService;
import com.example.jobagent.vo.MatchReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeMatchTool implements AgentTool {

    public static final String NAME = "resume_match";

    private final MatchService matchService;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Generate resume and job match report";
    }

    @Override
    public ToolResult<MatchReportVO> execute(ToolContext context) {
        try {
            MatchCreateDTO dto = new MatchCreateDTO();
            dto.setResumeId(context.getResumeId());
            dto.setJobPostId(context.getJobPostId());

            MatchReportVO report = matchService.create(context.getUserId(), dto);
            context.setMatchReport(report);
            context.setMatchReportId(report.getId());
            log.info("[AgentTool] success toolName={}, userId={}, resumeId={}, jobPostId={}, matchReportId={}",
                    getName(), context.getUserId(), context.getResumeId(), context.getJobPostId(), report.getId());
            return ToolResult.success(getName(), report);
        } catch (Exception e) {
            log.error("[AgentTool] failed toolName={}, userId={}, resumeId={}, jobPostId={}",
                    getName(), context.getUserId(), context.getResumeId(), context.getJobPostId(), e);
            return ToolResult.failure(getName(), "Resume match failed: " + e.getMessage());
        }
    }
}
