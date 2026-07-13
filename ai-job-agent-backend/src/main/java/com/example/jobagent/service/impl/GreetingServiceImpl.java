package com.example.jobagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.jobagent.agent.JobAgent;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.GreetingGenerateDTO;
import com.example.jobagent.dto.GreetingPageQueryDTO;
import com.example.jobagent.entity.GreetingRecord;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.mapper.GreetingRecordMapper;
import com.example.jobagent.mapper.JobAnalysisMapper;
import com.example.jobagent.mapper.JobPostMapper;
import com.example.jobagent.mapper.ResumeMapper;
import com.example.jobagent.service.GreetingService;
import com.example.jobagent.vo.GreetingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GreetingServiceImpl implements GreetingService {

    private final ResumeMapper resumeMapper;
    private final JobPostMapper jobPostMapper;
    private final JobAnalysisMapper jobAnalysisMapper;
    private final GreetingRecordMapper greetingRecordMapper;
    private final JobAgent jobAgent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GreetingVO generate(Long userId, GreetingGenerateDTO generateDTO) {
        Resume resume = getOwnedResume(userId, generateDTO.getResumeId());
        JobPost jobPost = getOwnedJobPost(userId, generateDTO.getJobPostId());
        JobAnalysis jobAnalysis = getLatestJobAnalysis(userId, generateDTO.getJobPostId());
        if (jobAnalysis == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位还没有分析结果，请先调用岗位分析接口");
        }

        GreetingRecord record = jobAgent.generateGreeting(userId, resume, jobPost, jobAnalysis);
        record.setUserId(userId);
        record.setResumeId(resume.getId());
        record.setJobPostId(jobPost.getId());
        greetingRecordMapper.insert(record);

        return toVO(record);
    }

    @Override
    public PageResult<GreetingVO> page(Long userId, GreetingPageQueryDTO queryDTO) {
        LambdaQueryWrapper<GreetingRecord> queryWrapper = new LambdaQueryWrapper<GreetingRecord>()
                .eq(GreetingRecord::getUserId, userId)
                .orderByDesc(GreetingRecord::getCreateTime);

        Page<GreetingRecord> page = greetingRecordMapper.selectPage(
                new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize()),
                queryWrapper
        );
        List<GreetingVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page, records);
    }

    @Override
    public GreetingVO getDetail(Long userId, Long id) {
        return toVO(getOwnedGreetingRecord(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwnedGreetingRecord(userId, id);
        int rows = greetingRecordMapper.delete(new LambdaQueryWrapper<GreetingRecord>()
                .eq(GreetingRecord::getId, id)
                .eq(GreetingRecord::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "打招呼话术记录不存在或无权访问");
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

    private GreetingRecord getOwnedGreetingRecord(Long userId, Long id) {
        GreetingRecord record = greetingRecordMapper.selectOne(new LambdaQueryWrapper<GreetingRecord>()
                .eq(GreetingRecord::getId, id)
                .eq(GreetingRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "打招呼话术记录不存在或无权访问");
        }
        return record;
    }

    private GreetingVO toVO(GreetingRecord record) {
        return GreetingVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .resumeId(record.getResumeId())
                .jobPostId(record.getJobPostId())
                .greetingText(record.getGreetingText())
                .rawResult(record.getRawResult())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }
}
