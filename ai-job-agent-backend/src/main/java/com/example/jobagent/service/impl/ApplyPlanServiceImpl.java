package com.example.jobagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.jobagent.agent.OneClickApplyAgent;
import com.example.jobagent.agent.OneClickApplyResult;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.ApplyPlanCreateDTO;
import com.example.jobagent.dto.ApplyPlanPageQueryDTO;
import com.example.jobagent.entity.ApplyPlanRecord;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.mapper.ApplyPlanRecordMapper;
import com.example.jobagent.service.ApplicationRecordService;
import com.example.jobagent.service.ApplyPlanService;
import com.example.jobagent.service.GreetingService;
import com.example.jobagent.service.InterviewService;
import com.example.jobagent.service.KnowledgeService;
import com.example.jobagent.service.MatchService;
import com.example.jobagent.service.ResumeRewriteService;
import com.example.jobagent.vo.ApplicationRecordVO;
import com.example.jobagent.vo.ApplyPlanVO;
import com.example.jobagent.vo.GreetingVO;
import com.example.jobagent.vo.InterviewQuestionVO;
import com.example.jobagent.vo.MatchReportVO;
import com.example.jobagent.vo.ResumeRewriteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyPlanServiceImpl implements ApplyPlanService {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private final ApplyPlanRecordMapper applyPlanRecordMapper;
    private final OneClickApplyAgent oneClickApplyAgent;
    private final MatchService matchService;
    private final ResumeRewriteService resumeRewriteService;
    private final GreetingService greetingService;
    private final InterviewService interviewService;
    private final ApplicationRecordService applicationRecordService;
    private final KnowledgeService knowledgeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplyPlanVO generate(Long userId, ApplyPlanCreateDTO createDTO) {
        boolean forceRegenerate = Boolean.TRUE.equals(createDTO.getForceRegenerate());
        log.info("[ApplyPlanService] generate start userId={}, resumeId={}, jobPostId={}, forceRegenerate={}",
                userId, createDTO.getResumeId(), createDTO.getJobPostId(), forceRegenerate);

        if (!forceRegenerate) {
            ApplyPlanVO cachedPlan = getLatestCompletePlan(userId, createDTO.getResumeId(), createDTO.getJobPostId());
            if (cachedPlan != null) {
                log.info("[ApplyPlanService] reuse existing apply plan userId={}, resumeId={}, jobPostId={}, applyPlanId={}",
                        userId, createDTO.getResumeId(), createDTO.getJobPostId(), cachedPlan.getId());
                return cachedPlan;
            }
        }

        OneClickApplyResult result = oneClickApplyAgent.execute(userId, createDTO.getResumeId(), createDTO.getJobPostId());

        ApplyPlanRecord record = new ApplyPlanRecord();
        record.setUserId(userId);
        record.setResumeId(createDTO.getResumeId());
        record.setJobPostId(createDTO.getJobPostId());
        record.setMatchReportId(getId(result.getMatchReport()));
        record.setResumeRewriteRecordId(getId(result.getResumeRewrite()));
        record.setGreetingRecordId(getId(result.getGreeting()));
        record.setInterviewQuestionRecordId(getId(result.getInterviewQuestions()));
        record.setApplicationRecordId(getId(result.getApplicationRecord()));
        record.setStatus(STATUS_SUCCESS);
        record.setNextStepSuggestion(result.getNextStepSuggestion());
        applyPlanRecordMapper.insert(record);

        log.info("[ApplyPlanService] generate success userId={}, resumeId={}, jobPostId={}, applyPlanId={}, matchReportId={}, applicationRecordId={}",
                userId,
                createDTO.getResumeId(),
                createDTO.getJobPostId(),
                record.getId(),
                record.getMatchReportId(),
                record.getApplicationRecordId());
        indexApplyPlanKnowledge(record, result);

        return toVO(record, result);
    }

    private ApplyPlanVO getLatestCompletePlan(Long userId, Long resumeId, Long jobPostId) {
        ApplyPlanRecord record = applyPlanRecordMapper.selectOne(new LambdaQueryWrapper<ApplyPlanRecord>()
                .eq(ApplyPlanRecord::getUserId, userId)
                .eq(ApplyPlanRecord::getResumeId, resumeId)
                .eq(ApplyPlanRecord::getJobPostId, jobPostId)
                .eq(ApplyPlanRecord::getStatus, STATUS_SUCCESS)
                .isNotNull(ApplyPlanRecord::getMatchReportId)
                .isNotNull(ApplyPlanRecord::getResumeRewriteRecordId)
                .isNotNull(ApplyPlanRecord::getGreetingRecordId)
                .isNotNull(ApplyPlanRecord::getInterviewQuestionRecordId)
                .isNotNull(ApplyPlanRecord::getApplicationRecordId)
                .orderByDesc(ApplyPlanRecord::getCreateTime)
                .last("LIMIT 1"));
        if (record == null) {
            return null;
        }
        try {
            return toVO(record, loadResult(userId, record));
        } catch (Exception e) {
            log.warn("[ApplyPlanService] latest complete apply plan cannot be loaded, will regenerate userId={}, resumeId={}, jobPostId={}, applyPlanId={}",
                    userId, resumeId, jobPostId, record.getId(), e);
            return null;
        }
    }

    @Override
    public PageResult<ApplyPlanVO> page(Long userId, ApplyPlanPageQueryDTO queryDTO) {
        LambdaQueryWrapper<ApplyPlanRecord> queryWrapper = new LambdaQueryWrapper<ApplyPlanRecord>()
                .eq(ApplyPlanRecord::getUserId, userId)
                .orderByDesc(ApplyPlanRecord::getCreateTime);

        Page<ApplyPlanRecord> page = applyPlanRecordMapper.selectPage(
                new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize()),
                queryWrapper
        );
        List<ApplyPlanVO> records = page.getRecords().stream().map(this::toSimpleVO).toList();
        return PageResult.of(page, records);
    }

    @Override
    public ApplyPlanVO getDetail(Long userId, Long id) {
        ApplyPlanRecord record = getOwnedRecord(userId, id);
        return toVO(record, loadResult(userId, record));
    }

    private ApplyPlanRecord getOwnedRecord(Long userId, Long id) {
        ApplyPlanRecord record = applyPlanRecordMapper.selectOne(new LambdaQueryWrapper<ApplyPlanRecord>()
                .eq(ApplyPlanRecord::getId, id)
                .eq(ApplyPlanRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Apply plan not found or no permission");
        }
        return record;
    }

    private OneClickApplyResult loadResult(Long userId, ApplyPlanRecord record) {
        return OneClickApplyResult.builder()
                .matchReport(record.getMatchReportId() == null ? null : matchService.getDetail(userId, record.getMatchReportId()))
                .resumeRewrite(record.getResumeRewriteRecordId() == null ? null : resumeRewriteService.getDetail(userId, record.getResumeRewriteRecordId()))
                .greeting(record.getGreetingRecordId() == null ? null : greetingService.getDetail(userId, record.getGreetingRecordId()))
                .interviewQuestions(record.getInterviewQuestionRecordId() == null ? null : interviewService.getDetail(userId, record.getInterviewQuestionRecordId()))
                .applicationRecord(record.getApplicationRecordId() == null ? null : applicationRecordService.getDetail(userId, record.getApplicationRecordId()))
                .nextStepSuggestion(record.getNextStepSuggestion())
                .build();
    }

    private ApplyPlanVO toSimpleVO(ApplyPlanRecord record) {
        return ApplyPlanVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .resumeId(record.getResumeId())
                .jobPostId(record.getJobPostId())
                .matchReportId(record.getMatchReportId())
                .resumeRewriteRecordId(record.getResumeRewriteRecordId())
                .greetingRecordId(record.getGreetingRecordId())
                .interviewQuestionRecordId(record.getInterviewQuestionRecordId())
                .applicationRecordId(record.getApplicationRecordId())
                .status(record.getStatus())
                .nextStepSuggestion(record.getNextStepSuggestion())
                .errorMessage(record.getErrorMessage())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private ApplyPlanVO toVO(ApplyPlanRecord record, OneClickApplyResult result) {
        return ApplyPlanVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .resumeId(record.getResumeId())
                .jobPostId(record.getJobPostId())
                .matchReportId(record.getMatchReportId())
                .resumeRewriteRecordId(record.getResumeRewriteRecordId())
                .greetingRecordId(record.getGreetingRecordId())
                .interviewQuestionRecordId(record.getInterviewQuestionRecordId())
                .applicationRecordId(record.getApplicationRecordId())
                .status(record.getStatus())
                .nextStepSuggestion(record.getNextStepSuggestion())
                .errorMessage(record.getErrorMessage())
                .matchReport(result.getMatchReport())
                .resumeRewrite(result.getResumeRewrite())
                .greeting(result.getGreeting())
                .interviewQuestions(result.getInterviewQuestions())
                .applicationRecord(result.getApplicationRecord())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private Long getId(MatchReportVO vo) {
        return vo == null ? null : vo.getId();
    }

    private Long getId(ResumeRewriteVO vo) {
        return vo == null ? null : vo.getId();
    }

    private Long getId(GreetingVO vo) {
        return vo == null ? null : vo.getId();
    }

    private Long getId(InterviewQuestionVO vo) {
        return vo == null ? null : vo.getId();
    }

    private Long getId(ApplicationRecordVO vo) {
        return vo == null ? null : vo.getId();
    }

    private void indexApplyPlanKnowledge(ApplyPlanRecord record, OneClickApplyResult result) {
        if (record == null || result == null) {
            return;
        }
        try {
            knowledgeService.indexKnowledge(
                    record.getUserId(),
                    "APPLY_PLAN",
                    record.getId(),
                    "Apply Plan " + record.getId(),
                    buildApplyPlanKnowledgeContent(record, result)
            );
        } catch (Exception e) {
            log.warn("[KnowledgeIndex] apply plan index failed userId={}, applyPlanId={}",
                    record.getUserId(), record.getId(), e);
        }
    }

    private String buildApplyPlanKnowledgeContent(ApplyPlanRecord record, OneClickApplyResult result) {
        MatchReportVO match = result.getMatchReport();
        ResumeRewriteVO rewrite = result.getResumeRewrite();
        GreetingVO greeting = result.getGreeting();
        InterviewQuestionVO interview = result.getInterviewQuestions();
        ApplicationRecordVO application = result.getApplicationRecord();

        return "一键求职方案ID：" + record.getId() + "\n"
                + "简历ID：" + record.getResumeId() + "\n"
                + "岗位ID：" + record.getJobPostId() + "\n"
                + "公司：" + defaultIfBlank(application == null ? null : application.getCompanyName(), "") + "\n"
                + "岗位：" + defaultIfBlank(application == null ? null : application.getJobName(), "") + "\n"
                + "匹配分：" + (match == null ? "" : match.getOverallScore()) + "\n"
                + "匹配优势：" + defaultIfBlank(match == null ? null : match.getAdvantageAnalysis(), "") + "\n"
                + "匹配不足：" + defaultIfBlank(match == null ? null : match.getWeaknessAnalysis(), "") + "\n"
                + "补强建议：" + defaultIfBlank(match == null ? null : match.getSuggestion(), "") + "\n"
                + "简历优化版本：" + defaultIfBlank(rewrite == null ? null : rewrite.getResumeVersion(), "") + "\n"
                + "打招呼话术：" + defaultIfBlank(greeting == null ? null : greeting.getGreetingText(), "") + "\n"
                + "技术面试题：" + defaultIfBlank(interview == null ? null : interview.getTechnicalQuestions(), "") + "\n"
                + "项目追问题：" + defaultIfBlank(interview == null ? null : interview.getProjectQuestions(), "") + "\n"
                + "HR问题：" + defaultIfBlank(interview == null ? null : interview.getHrQuestions(), "") + "\n"
                + "下一步建议：" + defaultIfBlank(result.getNextStepSuggestion(), "");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
