package com.example.jobagent.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.jobagent.agent.rag.RagContextBuilder;
import com.example.jobagent.agent.rag.RagContextHolder;
import com.example.jobagent.agent.tool.AgentTool;
import com.example.jobagent.agent.tool.ToolContext;
import com.example.jobagent.agent.tool.ToolRegistry;
import com.example.jobagent.agent.tool.ToolResult;
import com.example.jobagent.agent.tool.impl.ApplicationRecordTool;
import com.example.jobagent.agent.tool.impl.GreetingGenerateTool;
import com.example.jobagent.agent.tool.impl.InterviewQuestionTool;
import com.example.jobagent.agent.tool.impl.ResumeMatchTool;
import com.example.jobagent.agent.tool.impl.ResumeRewriteTool;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.mapper.JobAnalysisMapper;
import com.example.jobagent.mapper.JobPostMapper;
import com.example.jobagent.mapper.ResumeMapper;
import com.example.jobagent.service.KnowledgeService;
import com.example.jobagent.vo.ApplicationRecordVO;
import com.example.jobagent.vo.GreetingVO;
import com.example.jobagent.vo.InterviewQuestionVO;
import com.example.jobagent.vo.MatchReportVO;
import com.example.jobagent.vo.ResumeRewriteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class OneClickApplyAgent {

    private final ResumeMapper resumeMapper;
    private final JobPostMapper jobPostMapper;
    private final JobAnalysisMapper jobAnalysisMapper;
    private final JobAgent jobAgent;
    private final ToolRegistry toolRegistry;
    private final RagContextBuilder ragContextBuilder;
    private final KnowledgeService knowledgeService;

    public OneClickApplyResult execute(Long userId, Long resumeId, Long jobPostId) {
        log.info("[OneClickApplyAgent] start userId={}, resumeId={}, jobPostId={}", userId, resumeId, jobPostId);

        Resume resume = getOwnedResume(userId, resumeId);
        JobPost jobPost = getOwnedJobPost(userId, jobPostId);
        JobAnalysis jobAnalysis = ensureJobAnalysis(userId, jobPost);
        log.info("[OneClickApplyAgent] job analysis ready userId={}, jobPostId={}, jobAnalysisId={}",
                userId, jobPostId, jobAnalysis.getId());
        String ragContext = ragContextBuilder.build(userId, resume, jobPost, jobAnalysis);

        ToolContext context = ToolContext.builder()
                .userId(userId)
                .resumeId(resumeId)
                .jobPostId(jobPostId)
                .jobAnalysisId(jobAnalysis.getId())
                .resume(resume)
                .jobPost(jobPost)
                .jobAnalysis(jobAnalysis)
                .ragContext(ragContext)
                .build();
        context.getVariables().put("ragContext", ragContext);

        RagContextHolder.set(ragContext);
        try {
            MatchReportVO matchReport = executeTool(ResumeMatchTool.NAME, context, MatchReportVO.class);
            ResumeRewriteVO resumeRewrite = executeTool(ResumeRewriteTool.NAME, context, ResumeRewriteVO.class);
            GreetingVO greeting = executeTool(GreetingGenerateTool.NAME, context, GreetingVO.class);
            InterviewQuestionVO interviewQuestions = executeTool(InterviewQuestionTool.NAME, context, InterviewQuestionVO.class);
            ApplicationRecordVO applicationRecord = executeTool(ApplicationRecordTool.NAME, context, ApplicationRecordVO.class);

            log.info("[OneClickApplyAgent] success userId={}, resumeId={}, jobPostId={}, jobAnalysisId={}, matchReportId={}, applicationRecordId={}",
                    userId, resumeId, jobPostId, jobAnalysis.getId(), matchReport.getId(), applicationRecord.getId());

            return OneClickApplyResult.builder()
                    .matchReport(matchReport)
                    .resumeRewrite(resumeRewrite)
                    .greeting(greeting)
                    .interviewQuestions(interviewQuestions)
                    .applicationRecord(applicationRecord)
                    .nextStepSuggestion(buildNextStepSuggestion(matchReport))
                    .build();
        } finally {
            RagContextHolder.clear();
        }
    }

    private <T> T executeTool(String toolName, ToolContext context, Class<T> dataType) {
        AgentTool tool = toolRegistry.getTool(toolName);
        log.info("[OneClickApplyAgent] execute toolName={}, userId={}, resumeId={}, jobPostId={}",
                toolName, context.getUserId(), context.getResumeId(), context.getJobPostId());

        ToolResult<?> result = tool.execute(context);
        if (!result.isSuccess()) {
            log.error("[OneClickApplyAgent] tool failed toolName={}, userId={}, resumeId={}, jobPostId={}, error={}",
                    toolName, context.getUserId(), context.getResumeId(), context.getJobPostId(), result.getErrorMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, result.getErrorMessage());
        }
        Object data = result.getData();
        if (data == null) {
            String message = "Agent tool returned empty data: " + toolName;
            log.error("[OneClickApplyAgent] tool empty data toolName={}, userId={}, resumeId={}, jobPostId={}",
                    toolName, context.getUserId(), context.getResumeId(), context.getJobPostId());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, message);
        }
        return dataType.cast(data);
    }

    private JobAnalysis ensureJobAnalysis(Long userId, JobPost jobPost) {
        JobAnalysis analysis = getLatestJobAnalysis(userId, jobPost.getId());
        if (analysis != null) {
            return analysis;
        }

        log.warn("[OneClickApplyAgent] job analysis missing, auto analyzing userId={}, jobPostId={}",
                userId, jobPost.getId());
        if (!StringUtils.hasText(jobPost.getJdText())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Job analysis not found and job JD is empty");
        }

        try {
            JobAnalysis newAnalysis = jobAgent.parseJob(userId, jobPost.getJdText());
            newAnalysis.setUserId(userId);
            newAnalysis.setJobPostId(jobPost.getId());
            jobAnalysisMapper.insert(newAnalysis);

            jobPost.setCompanyName(defaultIfBlank(newAnalysis.getCompanyName(), jobPost.getCompanyName()));
            jobPost.setJobName(defaultIfBlank(newAnalysis.getJobName(), jobPost.getJobName()));
            jobPost.setCity(defaultIfBlank(newAnalysis.getCity(), jobPost.getCity()));
            jobPost.setSalary(defaultIfBlank(newAnalysis.getSalary(), jobPost.getSalary()));
            jobPostMapper.updateById(jobPost);

            log.info("[OneClickApplyAgent] auto analysis created userId={}, jobPostId={}, jobAnalysisId={}",
                    userId, jobPost.getId(), newAnalysis.getId());
            indexAutoJobAnalysisKnowledge(jobPost, newAnalysis);
            return newAnalysis;
        } catch (BusinessException e) {
            log.error("[OneClickApplyAgent] auto analysis failed userId={}, jobPostId={}", userId, jobPost.getId(), e);
            throw new BusinessException(e.getErrorCode(), "Job analysis auto generation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("[OneClickApplyAgent] auto analysis failed userId={}, jobPostId={}", userId, jobPost.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Job analysis auto generation failed: " + e.getMessage());
        }
    }

    private JobAnalysis getLatestJobAnalysis(Long userId, Long jobPostId) {
        return jobAnalysisMapper.selectOne(new LambdaQueryWrapper<JobAnalysis>()
                .eq(JobAnalysis::getUserId, userId)
                .eq(JobAnalysis::getJobPostId, jobPostId)
                .orderByDesc(JobAnalysis::getId)
                .last("LIMIT 1"));
    }

    private Resume getOwnedResume(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getId, resumeId)
                .eq(Resume::getUserId, userId));
        if (resume == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Resume not found or not owned by current user");
        }
        return resume;
    }

    private JobPost getOwnedJobPost(Long userId, Long jobPostId) {
        JobPost jobPost = jobPostMapper.selectOne(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getId, jobPostId)
                .eq(JobPost::getUserId, userId));
        if (jobPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Job post not found or not owned by current user");
        }
        return jobPost;
    }

    private String buildNextStepSuggestion(MatchReportVO matchReport) {
        Integer score = matchReport.getOverallScore();
        boolean recommended = Integer.valueOf(1).equals(matchReport.getIsRecommended());
        if (score != null && score >= 85 && recommended) {
            return "Match score is high. Prioritize this application, merge the rewritten project experience into your resume, send the greeting, and prepare interview details.";
        }
        if (score != null && score >= 70) {
            return "This job is worth applying to. Improve the resume based on the suggestions first, then apply and track communication progress.";
        }
        return "Apply cautiously. Consider adding a closer project demo and improving your tech stack and project experience before applying.";
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private void indexAutoJobAnalysisKnowledge(JobPost jobPost, JobAnalysis analysis) {
        try {
            knowledgeService.indexKnowledge(
                    analysis.getUserId(),
                    "JOB_ANALYSIS",
                    analysis.getId(),
                    defaultIfBlank(analysis.getCompanyName(), "") + " " + defaultIfBlank(analysis.getJobName(), "Job Analysis"),
                    "Company: " + defaultIfBlank(analysis.getCompanyName(), jobPost.getCompanyName()) + "\n"
                            + "Job: " + defaultIfBlank(analysis.getJobName(), jobPost.getJobName()) + "\n"
                            + "Tech Stack: " + defaultIfBlank(analysis.getTechStack(), "") + "\n"
                            + "Responsibilities: " + defaultIfBlank(analysis.getResponsibilities(), "") + "\n"
                            + "Requirements: " + defaultIfBlank(analysis.getRequirements(), "") + "\n"
                            + "Bonus Points: " + defaultIfBlank(analysis.getBonusPoints(), "") + "\n"
                            + "Risk Points: " + defaultIfBlank(analysis.getRiskPoints(), "") + "\n"
                            + "JD: " + defaultIfBlank(jobPost.getJdText(), "")
            );
        } catch (Exception e) {
            log.warn("[KnowledgeIndex] auto job analysis index failed userId={}, jobPostId={}, jobAnalysisId={}",
                    analysis.getUserId(), jobPost.getId(), analysis.getId(), e);
        }
    }
}
