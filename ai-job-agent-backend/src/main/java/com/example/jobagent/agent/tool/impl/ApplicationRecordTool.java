package com.example.jobagent.agent.tool.impl;

import com.example.jobagent.agent.tool.AgentTool;
import com.example.jobagent.agent.tool.ToolContext;
import com.example.jobagent.agent.tool.ToolResult;
import com.example.jobagent.dto.ApplicationCreateDTO;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.enums.ApplicationStatus;
import com.example.jobagent.service.ApplicationRecordService;
import com.example.jobagent.vo.ApplicationRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationRecordTool implements AgentTool {

    public static final String NAME = "application_record";

    private final ApplicationRecordService applicationRecordService;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Create application record for target job";
    }

    @Override
    public ToolResult<ApplicationRecordVO> execute(ToolContext context) {
        try {
            JobPost jobPost = context.getJobPost();
            ApplicationCreateDTO dto = new ApplicationCreateDTO();
            dto.setResumeId(context.getResumeId());
            dto.setJobPostId(context.getJobPostId());
            dto.setMatchReportId(context.getMatchReportId());
            dto.setCompanyName(defaultIfBlank(jobPost == null ? null : jobPost.getCompanyName(), "Unknown company"));
            dto.setJobName(defaultIfBlank(jobPost == null ? null : jobPost.getJobName(), "Unknown job"));
            dto.setCity(jobPost == null ? null : jobPost.getCity());
            dto.setSalary(jobPost == null ? null : jobPost.getSalary());
            dto.setJdText(jobPost == null ? null : jobPost.getJdText());
            dto.setMatchScore(context.getMatchReport() == null ? null : context.getMatchReport().getOverallScore());
            dto.setStatus(ApplicationStatus.NOT_APPLIED.name());
            dto.setRemark("Created by one click apply agent.");

            ApplicationRecordVO applicationRecord = applicationRecordService.create(context.getUserId(), dto);
            context.setApplicationRecord(applicationRecord);
            context.setApplicationRecordId(applicationRecord.getId());
            log.info("[AgentTool] success toolName={}, userId={}, resumeId={}, jobPostId={}, applicationRecordId={}",
                    getName(), context.getUserId(), context.getResumeId(), context.getJobPostId(), applicationRecord.getId());
            return ToolResult.success(getName(), applicationRecord);
        } catch (Exception e) {
            log.error("[AgentTool] failed toolName={}, userId={}, resumeId={}, jobPostId={}",
                    getName(), context.getUserId(), context.getResumeId(), context.getJobPostId(), e);
            return ToolResult.failure(getName(), "Application record creation failed: " + e.getMessage());
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
