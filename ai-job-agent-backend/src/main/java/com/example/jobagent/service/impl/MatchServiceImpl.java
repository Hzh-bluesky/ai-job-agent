package com.example.jobagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.jobagent.agent.JobAgent;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.MatchCreateDTO;
import com.example.jobagent.dto.MatchPageQueryDTO;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.MatchReport;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.mapper.JobAnalysisMapper;
import com.example.jobagent.mapper.JobPostMapper;
import com.example.jobagent.mapper.MatchReportMapper;
import com.example.jobagent.mapper.ResumeMapper;
import com.example.jobagent.service.KnowledgeService;
import com.example.jobagent.service.MatchService;
import com.example.jobagent.vo.MatchReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final ResumeMapper resumeMapper;
    private final JobPostMapper jobPostMapper;
    private final JobAnalysisMapper jobAnalysisMapper;
    private final MatchReportMapper matchReportMapper;
    private final JobAgent jobAgent;
    private final KnowledgeService knowledgeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchReportVO create(Long userId, MatchCreateDTO createDTO) {
        Resume resume = getOwnedResume(userId, createDTO.getResumeId());
        JobPost jobPost = getOwnedJobPost(userId, createDTO.getJobPostId());
        JobAnalysis jobAnalysis = getLatestJobAnalysis(userId, createDTO.getJobPostId());
        if (jobAnalysis == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位还没有分析结果，请先调用岗位分析接口");
        }

        MatchReport report = jobAgent.matchResume(userId, resume, jobPost, jobAnalysis);
        report.setUserId(userId);
        report.setResumeId(resume.getId());
        report.setJobPostId(jobPost.getId());
        report.setJobAnalysisId(jobAnalysis.getId());
        matchReportMapper.insert(report);
        indexMatchReportKnowledge(report, resume, jobPost);

        return toVO(report);
    }

    @Override
    public MatchReportVO getDetail(Long userId, Long id) {
        return toVO(getOwnedMatchReport(userId, id));
    }

    @Override
    public PageResult<MatchReportVO> page(Long userId, MatchPageQueryDTO queryDTO) {
        LambdaQueryWrapper<MatchReport> queryWrapper = new LambdaQueryWrapper<MatchReport>()
                .eq(MatchReport::getUserId, userId)
                .orderByDesc(MatchReport::getCreateTime);

        Page<MatchReport> page = matchReportMapper.selectPage(
                new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize()),
                queryWrapper
        );
        List<MatchReportVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwnedMatchReport(userId, id);
        int rows = matchReportMapper.delete(new LambdaQueryWrapper<MatchReport>()
                .eq(MatchReport::getId, id)
                .eq(MatchReport::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "匹配报告不存在或无权访问");
        }
    }

    private Resume getOwnedResume(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getId, resumeId)
                .eq(Resume::getUserId, userId));
        if (resume == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "简历不存在或无权访问");
        }
        return resume;
    }

    private JobPost getOwnedJobPost(Long userId, Long jobPostId) {
        JobPost jobPost = jobPostMapper.selectOne(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getId, jobPostId)
                .eq(JobPost::getUserId, userId));
        if (jobPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位不存在或无权访问");
        }
        return jobPost;
    }

    private JobAnalysis getLatestJobAnalysis(Long userId, Long jobPostId) {
        return jobAnalysisMapper.selectOne(new LambdaQueryWrapper<JobAnalysis>()
                .eq(JobAnalysis::getUserId, userId)
                .eq(JobAnalysis::getJobPostId, jobPostId)
                .orderByDesc(JobAnalysis::getId)
                .last("LIMIT 1"));
    }

    private MatchReport getOwnedMatchReport(Long userId, Long id) {
        MatchReport report = matchReportMapper.selectOne(new LambdaQueryWrapper<MatchReport>()
                .eq(MatchReport::getId, id)
                .eq(MatchReport::getUserId, userId));
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "匹配报告不存在或无权访问");
        }
        return report;
    }

    private MatchReportVO toVO(MatchReport report) {
        return MatchReportVO.builder()
                .id(report.getId())
                .userId(report.getUserId())
                .resumeId(report.getResumeId())
                .jobPostId(report.getJobPostId())
                .jobAnalysisId(report.getJobAnalysisId())
                .overallScore(report.getOverallScore())
                .techScore(report.getTechScore())
                .projectScore(report.getProjectScore())
                .educationScore(report.getEducationScore())
                .advantageAnalysis(report.getAdvantageAnalysis())
                .weaknessAnalysis(report.getWeaknessAnalysis())
                .suggestion(report.getSuggestion())
                .isRecommended(report.getIsRecommended())
                .rawResult(report.getRawResult())
                .createTime(report.getCreateTime())
                .updateTime(report.getUpdateTime())
                .build();
    }

    private void indexMatchReportKnowledge(MatchReport report, Resume resume, JobPost jobPost) {
        if (report == null) {
            return;
        }
        try {
            knowledgeService.indexKnowledge(
                    report.getUserId(),
                    "MATCH_REPORT",
                    report.getId(),
                    "Match " + defaultIfBlank(jobPost == null ? null : jobPost.getJobName(), "Report"),
                    buildMatchReportKnowledgeContent(report, resume, jobPost)
            );
        } catch (Exception e) {
            log.warn("[KnowledgeIndex] match report index failed userId={}, matchReportId={}",
                    report.getUserId(), report.getId(), e);
        }
    }

    private String buildMatchReportKnowledgeContent(MatchReport report, Resume resume, JobPost jobPost) {
        return "简历：" + defaultIfBlank(resume == null ? null : resume.getTitle(), "") + "\n"
                + "岗位：" + defaultIfBlank(jobPost == null ? null : jobPost.getJobName(), "") + "\n"
                + "综合分：" + report.getOverallScore() + "\n"
                + "技术栈分：" + report.getTechScore() + "\n"
                + "项目分：" + report.getProjectScore() + "\n"
                + "学历年级分：" + report.getEducationScore() + "\n"
                + "优势：" + defaultIfBlank(report.getAdvantageAnalysis(), "") + "\n"
                + "不足：" + defaultIfBlank(report.getWeaknessAnalysis(), "") + "\n"
                + "建议：" + defaultIfBlank(report.getSuggestion(), "");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
