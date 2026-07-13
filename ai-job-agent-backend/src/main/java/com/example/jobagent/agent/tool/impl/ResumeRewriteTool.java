package com.example.jobagent.agent.tool.impl;

import com.example.jobagent.agent.tool.AgentTool;
import com.example.jobagent.agent.tool.ToolContext;
import com.example.jobagent.agent.tool.ToolResult;
import com.example.jobagent.dto.ResumeRewriteDTO;
import com.example.jobagent.service.ResumeRewriteService;
import com.example.jobagent.vo.ResumeRewriteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeRewriteTool implements AgentTool {

    public static final String NAME = "resume_rewrite";

    private final ResumeRewriteService resumeRewriteService;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Rewrite resume project experience for target job";
    }

    @Override
    public ToolResult<ResumeRewriteVO> execute(ToolContext context) {
        try {
            ResumeRewriteDTO dto = new ResumeRewriteDTO();
            dto.setResumeId(context.getResumeId());
            dto.setJobPostId(context.getJobPostId());
            dto.setProjectExperience(defaultIfBlank(
                    context.getResume() == null ? null : context.getResume().getProjectExperience(),
                    "No project experience is provided. Please add real project experience before resume rewriting."
            ));

            ResumeRewriteVO rewrite = resumeRewriteService.create(context.getUserId(), dto);
            context.setResumeRewrite(rewrite);
            context.setResumeRewriteRecordId(rewrite.getId());
            log.info("[AgentTool] success toolName={}, userId={}, resumeId={}, jobPostId={}, resumeRewriteRecordId={}",
                    getName(), context.getUserId(), context.getResumeId(), context.getJobPostId(), rewrite.getId());
            return ToolResult.success(getName(), rewrite);
        } catch (Exception e) {
            log.error("[AgentTool] failed toolName={}, userId={}, resumeId={}, jobPostId={}",
                    getName(), context.getUserId(), context.getResumeId(), context.getJobPostId(), e);
            return ToolResult.failure(getName(), "Resume rewrite failed: " + e.getMessage());
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
