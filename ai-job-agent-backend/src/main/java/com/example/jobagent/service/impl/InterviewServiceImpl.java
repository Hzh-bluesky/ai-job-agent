package com.example.jobagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.jobagent.agent.JobAgent;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.InterviewGenerateDTO;
import com.example.jobagent.dto.InterviewPageQueryDTO;
import com.example.jobagent.entity.InterviewQuestionRecord;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.mapper.InterviewQuestionRecordMapper;
import com.example.jobagent.mapper.JobAnalysisMapper;
import com.example.jobagent.mapper.JobPostMapper;
import com.example.jobagent.service.InterviewService;
import com.example.jobagent.service.KnowledgeService;
import com.example.jobagent.vo.InterviewQuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final JobPostMapper jobPostMapper;
    private final JobAnalysisMapper jobAnalysisMapper;
    private final InterviewQuestionRecordMapper interviewQuestionRecordMapper;
    private final JobAgent jobAgent;
    private final KnowledgeService knowledgeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewQuestionVO generate(Long userId, InterviewGenerateDTO generateDTO) {
        JobPost jobPost = getOwnedJobPost(userId, generateDTO.getJobPostId());
        JobAnalysis jobAnalysis = getLatestJobAnalysis(userId, generateDTO.getJobPostId());
        if (jobAnalysis == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位还没有分析结果，请先调用岗位分析接口");
        }

        InterviewQuestionRecord record = jobAgent.generateInterviewQuestions(userId, jobPost, jobAnalysis);
        record.setUserId(userId);
        record.setJobPostId(jobPost.getId());
        interviewQuestionRecordMapper.insert(record);
        indexInterviewKnowledge(record, jobPost);

        return toVO(record);
    }

    @Override
    public PageResult<InterviewQuestionVO> page(Long userId, InterviewPageQueryDTO queryDTO) {
        LambdaQueryWrapper<InterviewQuestionRecord> queryWrapper = new LambdaQueryWrapper<InterviewQuestionRecord>()
                .eq(InterviewQuestionRecord::getUserId, userId)
                .orderByDesc(InterviewQuestionRecord::getCreateTime);

        Page<InterviewQuestionRecord> page = interviewQuestionRecordMapper.selectPage(
                new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize()),
                queryWrapper
        );
        List<InterviewQuestionVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page, records);
    }

    @Override
    public InterviewQuestionVO getDetail(Long userId, Long id) {
        return toVO(getOwnedInterviewRecord(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwnedInterviewRecord(userId, id);
        int rows = interviewQuestionRecordMapper.delete(new LambdaQueryWrapper<InterviewQuestionRecord>()
                .eq(InterviewQuestionRecord::getId, id)
                .eq(InterviewQuestionRecord::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "面试题记录不存在或无权访问");
        }
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

    private InterviewQuestionRecord getOwnedInterviewRecord(Long userId, Long id) {
        InterviewQuestionRecord record = interviewQuestionRecordMapper.selectOne(
                new LambdaQueryWrapper<InterviewQuestionRecord>()
                        .eq(InterviewQuestionRecord::getId, id)
                        .eq(InterviewQuestionRecord::getUserId, userId)
        );
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "面试题记录不存在或无权访问");
        }
        return record;
    }

    private InterviewQuestionVO toVO(InterviewQuestionRecord record) {
        return InterviewQuestionVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .jobPostId(record.getJobPostId())
                .technicalQuestions(record.getTechnicalQuestions())
                .projectQuestions(record.getProjectQuestions())
                .hrQuestions(record.getHrQuestions())
                .rawResult(record.getRawResult())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private void indexInterviewKnowledge(InterviewQuestionRecord record, JobPost jobPost) {
        if (record == null) {
            return;
        }
        try {
            knowledgeService.indexKnowledge(
                    record.getUserId(),
                    "INTERVIEW_QUESTION",
                    record.getId(),
                    "Interview " + defaultIfBlank(jobPost == null ? null : jobPost.getJobName(), "Questions"),
                    buildInterviewKnowledgeContent(record, jobPost)
            );
        } catch (Exception e) {
            log.warn("[KnowledgeIndex] interview question index failed userId={}, interviewId={}",
                    record.getUserId(), record.getId(), e);
        }
    }

    private String buildInterviewKnowledgeContent(InterviewQuestionRecord record, JobPost jobPost) {
        return "岗位：" + defaultIfBlank(jobPost == null ? null : jobPost.getJobName(), "") + "\n"
                + "技术题：" + defaultIfBlank(record.getTechnicalQuestions(), "") + "\n"
                + "项目追问：" + defaultIfBlank(record.getProjectQuestions(), "") + "\n"
                + "HR问题：" + defaultIfBlank(record.getHrQuestions(), "");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
