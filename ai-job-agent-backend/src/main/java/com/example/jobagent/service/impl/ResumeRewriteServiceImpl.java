package com.example.jobagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.jobagent.agent.JobAgent;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.ResumeRewriteDTO;
import com.example.jobagent.dto.ResumeRewritePageQueryDTO;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.entity.ResumeRewriteRecord;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.mapper.JobAnalysisMapper;
import com.example.jobagent.mapper.JobPostMapper;
import com.example.jobagent.mapper.ResumeMapper;
import com.example.jobagent.mapper.ResumeRewriteRecordMapper;
import com.example.jobagent.service.KnowledgeService;
import com.example.jobagent.service.ResumeRewriteService;
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
public class ResumeRewriteServiceImpl implements ResumeRewriteService {

    private final ResumeMapper resumeMapper;
    private final JobPostMapper jobPostMapper;
    private final JobAnalysisMapper jobAnalysisMapper;
    private final ResumeRewriteRecordMapper resumeRewriteRecordMapper;
    private final JobAgent jobAgent;
    private final KnowledgeService knowledgeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeRewriteVO create(Long userId, ResumeRewriteDTO rewriteDTO) {
        Resume resume = getOwnedResume(userId, rewriteDTO.getResumeId());
        JobPost jobPost = getOwnedJobPost(userId, rewriteDTO.getJobPostId());
        JobAnalysis jobAnalysis = getLatestJobAnalysis(userId, rewriteDTO.getJobPostId());
        if (jobAnalysis == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位还没有分析结果，请先调用岗位分析接口");
        }

        ResumeRewriteRecord record = jobAgent.rewriteResume(
                userId,
                resume,
                jobPost,
                jobAnalysis,
                rewriteDTO.getProjectExperience()
        );
        record.setUserId(userId);
        record.setResumeId(resume.getId());
        record.setJobPostId(jobPost.getId());
        record.setOriginalProject(rewriteDTO.getProjectExperience());
        resumeRewriteRecordMapper.insert(record);
        indexResumeRewriteKnowledge(record, resume, jobPost);

        return toVO(record);
    }

    @Override
    public PageResult<ResumeRewriteVO> page(Long userId, ResumeRewritePageQueryDTO queryDTO) {
        LambdaQueryWrapper<ResumeRewriteRecord> queryWrapper = new LambdaQueryWrapper<ResumeRewriteRecord>()
                .eq(ResumeRewriteRecord::getUserId, userId)
                .orderByDesc(ResumeRewriteRecord::getCreateTime);

        Page<ResumeRewriteRecord> page = resumeRewriteRecordMapper.selectPage(
                new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize()),
                queryWrapper
        );
        List<ResumeRewriteVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page, records);
    }

    @Override
    public ResumeRewriteVO getDetail(Long userId, Long id) {
        return toVO(getOwnedRewriteRecord(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwnedRewriteRecord(userId, id);
        int rows = resumeRewriteRecordMapper.delete(new LambdaQueryWrapper<ResumeRewriteRecord>()
                .eq(ResumeRewriteRecord::getId, id)
                .eq(ResumeRewriteRecord::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "简历优化记录不存在或无权访问");
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

    private ResumeRewriteRecord getOwnedRewriteRecord(Long userId, Long id) {
        ResumeRewriteRecord record = resumeRewriteRecordMapper.selectOne(new LambdaQueryWrapper<ResumeRewriteRecord>()
                .eq(ResumeRewriteRecord::getId, id)
                .eq(ResumeRewriteRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "简历优化记录不存在或无权访问");
        }
        return record;
    }

    private ResumeRewriteVO toVO(ResumeRewriteRecord record) {
        return ResumeRewriteVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .resumeId(record.getResumeId())
                .jobPostId(record.getJobPostId())
                .originalProject(record.getOriginalProject())
                .rewrittenProject(record.getRewrittenProject())
                .rewriteReason(record.getRewriteReason())
                .resumeVersion(record.getResumeVersion())
                .rawResult(record.getRawResult())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private void indexResumeRewriteKnowledge(ResumeRewriteRecord record, Resume resume, JobPost jobPost) {
        if (record == null) {
            return;
        }
        try {
            knowledgeService.indexKnowledge(
                    record.getUserId(),
                    "RESUME_REWRITE",
                    record.getId(),
                    "Rewrite " + defaultIfBlank(jobPost == null ? null : jobPost.getJobName(), "Resume Project"),
                    buildResumeRewriteKnowledgeContent(record, resume, jobPost)
            );
        } catch (Exception e) {
            log.warn("[KnowledgeIndex] resume rewrite index failed userId={}, rewriteId={}",
                    record.getUserId(), record.getId(), e);
        }
    }

    private String buildResumeRewriteKnowledgeContent(ResumeRewriteRecord record, Resume resume, JobPost jobPost) {
        return "简历：" + defaultIfBlank(resume == null ? null : resume.getTitle(), "") + "\n"
                + "岗位：" + defaultIfBlank(jobPost == null ? null : jobPost.getJobName(), "") + "\n"
                + "原始项目：" + defaultIfBlank(record.getOriginalProject(), "") + "\n"
                + "优化项目：" + defaultIfBlank(record.getRewrittenProject(), "") + "\n"
                + "优化理由：" + defaultIfBlank(record.getRewriteReason(), "") + "\n"
                + "简历版本：" + defaultIfBlank(record.getResumeVersion(), "");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
