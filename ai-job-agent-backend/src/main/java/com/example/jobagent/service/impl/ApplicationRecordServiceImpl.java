package com.example.jobagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.ApplicationCreateDTO;
import com.example.jobagent.dto.ApplicationPageQueryDTO;
import com.example.jobagent.dto.ApplicationStatusUpdateDTO;
import com.example.jobagent.dto.ApplicationUpdateDTO;
import com.example.jobagent.entity.ApplicationRecord;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.MatchReport;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.enums.ApplicationStatus;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.mapper.ApplicationRecordMapper;
import com.example.jobagent.mapper.JobPostMapper;
import com.example.jobagent.mapper.MatchReportMapper;
import com.example.jobagent.mapper.ResumeMapper;
import com.example.jobagent.service.ApplicationRecordService;
import com.example.jobagent.vo.ApplicationRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationRecordServiceImpl implements ApplicationRecordService {

    private final ApplicationRecordMapper applicationRecordMapper;
    private final ResumeMapper resumeMapper;
    private final JobPostMapper jobPostMapper;
    private final MatchReportMapper matchReportMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationRecordVO create(Long userId, ApplicationCreateDTO createDTO) {
        validateStatus(createDTO.getStatus());
        validateOwnedResumeIfPresent(userId, createDTO.getResumeId());
        validateOwnedJobPostIfPresent(userId, createDTO.getJobPostId());
        MatchReport matchReport = validateOwnedMatchReportIfPresent(userId, createDTO.getMatchReportId());
        validateMatchReportRelation(matchReport, createDTO.getResumeId(), createDTO.getJobPostId());

        ApplicationRecord record = new ApplicationRecord();
        record.setUserId(userId);
        record.setResumeId(createDTO.getResumeId() != null ? createDTO.getResumeId() : getResumeId(matchReport));
        record.setJobPostId(createDTO.getJobPostId() != null ? createDTO.getJobPostId() : getJobPostId(matchReport));
        record.setMatchReportId(createDTO.getMatchReportId());
        record.setCompanyName(createDTO.getCompanyName());
        record.setJobName(createDTO.getJobName());
        record.setCity(createDTO.getCity());
        record.setSalary(createDTO.getSalary());
        record.setJdText(createDTO.getJdText());
        record.setMatchScore(createDTO.getMatchScore() != null ? createDTO.getMatchScore() : getMatchScore(matchReport));
        record.setStatus(createDTO.getStatus());
        record.setRemark(createDTO.getRemark());
        applicationRecordMapper.insert(record);

        return toVO(record);
    }

    @Override
    public PageResult<ApplicationRecordVO> page(Long userId, ApplicationPageQueryDTO queryDTO) {
        LambdaQueryWrapper<ApplicationRecord> queryWrapper = new LambdaQueryWrapper<ApplicationRecord>()
                .eq(ApplicationRecord::getUserId, userId)
                .orderByDesc(ApplicationRecord::getUpdateTime);

        Page<ApplicationRecord> page = applicationRecordMapper.selectPage(
                new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize()),
                queryWrapper
        );
        List<ApplicationRecordVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page, records);
    }

    @Override
    public ApplicationRecordVO getDetail(Long userId, Long id) {
        return toVO(getOwnedApplicationRecord(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationRecordVO update(Long userId, Long id, ApplicationUpdateDTO updateDTO) {
        ApplicationRecord record = getOwnedApplicationRecord(userId, id);
        validateOwnedResumeIfPresent(userId, updateDTO.getResumeId());
        validateOwnedJobPostIfPresent(userId, updateDTO.getJobPostId());
        MatchReport matchReport = validateOwnedMatchReportIfPresent(userId, updateDTO.getMatchReportId());
        Long effectiveResumeId = updateDTO.getResumeId() != null ? updateDTO.getResumeId() : record.getResumeId();
        Long effectiveJobPostId = updateDTO.getJobPostId() != null ? updateDTO.getJobPostId() : record.getJobPostId();
        validateMatchReportRelation(matchReport, effectiveResumeId, effectiveJobPostId);

        if (updateDTO.getResumeId() != null) {
            record.setResumeId(updateDTO.getResumeId());
        }
        if (updateDTO.getJobPostId() != null) {
            record.setJobPostId(updateDTO.getJobPostId());
        }
        if (updateDTO.getMatchReportId() != null) {
            record.setMatchReportId(updateDTO.getMatchReportId());
        }
        if (updateDTO.getCompanyName() != null) {
            record.setCompanyName(updateDTO.getCompanyName());
        }
        if (updateDTO.getJobName() != null) {
            record.setJobName(updateDTO.getJobName());
        }
        if (updateDTO.getCity() != null) {
            record.setCity(updateDTO.getCity());
        }
        if (updateDTO.getSalary() != null) {
            record.setSalary(updateDTO.getSalary());
        }
        if (updateDTO.getJdText() != null) {
            record.setJdText(updateDTO.getJdText());
        }
        if (updateDTO.getMatchScore() != null) {
            record.setMatchScore(updateDTO.getMatchScore());
        }
        if (updateDTO.getRemark() != null) {
            record.setRemark(updateDTO.getRemark());
        }

        applicationRecordMapper.updateById(record);
        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationRecordVO updateStatus(Long userId, Long id, ApplicationStatusUpdateDTO statusUpdateDTO) {
        validateStatus(statusUpdateDTO.getStatus());
        ApplicationRecord record = getOwnedApplicationRecord(userId, id);
        record.setStatus(statusUpdateDTO.getStatus());
        applicationRecordMapper.updateById(record);
        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwnedApplicationRecord(userId, id);
        int rows = applicationRecordMapper.delete(new LambdaQueryWrapper<ApplicationRecord>()
                .eq(ApplicationRecord::getId, id)
                .eq(ApplicationRecord::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "投递记录不存在或无权访问");
        }
    }

    private void validateStatus(String status) {
        if (!ApplicationStatus.isValid(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "投递状态不合法");
        }
    }

    private void validateOwnedResumeIfPresent(Long userId, Long resumeId) {
        if (resumeId == null) {
            return;
        }
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getId, resumeId)
                .eq(Resume::getUserId, userId));
        if (resume == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "简历不存在或无权访问");
        }
    }

    private void validateOwnedJobPostIfPresent(Long userId, Long jobPostId) {
        if (jobPostId == null) {
            return;
        }
        JobPost jobPost = jobPostMapper.selectOne(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getId, jobPostId)
                .eq(JobPost::getUserId, userId));
        if (jobPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位不存在或无权访问");
        }
    }

    private MatchReport validateOwnedMatchReportIfPresent(Long userId, Long matchReportId) {
        if (matchReportId == null) {
            return null;
        }
        MatchReport matchReport = matchReportMapper.selectOne(new LambdaQueryWrapper<MatchReport>()
                .eq(MatchReport::getId, matchReportId)
                .eq(MatchReport::getUserId, userId));
        if (matchReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "匹配报告不存在或无权访问");
        }
        return matchReport;
    }

    private ApplicationRecord getOwnedApplicationRecord(Long userId, Long id) {
        ApplicationRecord record = applicationRecordMapper.selectOne(new LambdaQueryWrapper<ApplicationRecord>()
                .eq(ApplicationRecord::getId, id)
                .eq(ApplicationRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "投递记录不存在或无权访问");
        }
        return record;
    }

    private Integer getMatchScore(MatchReport matchReport) {
        return matchReport == null ? null : matchReport.getOverallScore();
    }

    private Long getResumeId(MatchReport matchReport) {
        return matchReport == null ? null : matchReport.getResumeId();
    }

    private Long getJobPostId(MatchReport matchReport) {
        return matchReport == null ? null : matchReport.getJobPostId();
    }

    private void validateMatchReportRelation(MatchReport matchReport, Long resumeId, Long jobPostId) {
        if (matchReport == null) {
            return;
        }
        if (resumeId != null && !resumeId.equals(matchReport.getResumeId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "简历和匹配报告不一致");
        }
        if (jobPostId != null && !jobPostId.equals(matchReport.getJobPostId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位和匹配报告不一致");
        }
    }

    private ApplicationRecordVO toVO(ApplicationRecord record) {
        return ApplicationRecordVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .resumeId(record.getResumeId())
                .jobPostId(record.getJobPostId())
                .matchReportId(record.getMatchReportId())
                .companyName(record.getCompanyName())
                .jobName(record.getJobName())
                .city(record.getCity())
                .salary(record.getSalary())
                .jdText(record.getJdText())
                .matchScore(record.getMatchScore())
                .status(record.getStatus())
                .remark(record.getRemark())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }
}
