package com.example.jobagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.ResumeCreateDTO;
import com.example.jobagent.dto.ResumePageQueryDTO;
import com.example.jobagent.dto.ResumeUpdateDTO;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.mapper.ResumeMapper;
import com.example.jobagent.service.KnowledgeService;
import com.example.jobagent.service.ResumeService;
import com.example.jobagent.vo.ResumeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final int DEFAULT_YES = 1;
    private static final int DEFAULT_NO = 0;

    private final ResumeMapper resumeMapper;
    private final KnowledgeService knowledgeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO create(Long userId, ResumeCreateDTO createDTO) {
        Integer isDefault = resolveCreateDefaultValue(userId, createDTO.getIsDefault());
        if (DEFAULT_YES == isDefault) {
            clearUserDefaultResume(userId);
        }

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(createDTO.getTitle());
        resume.setName(createDTO.getName());
        resume.setSchool(createDTO.getSchool());
        resume.setMajor(createDTO.getMajor());
        resume.setGrade(createDTO.getGrade());
        resume.setTechStack(createDTO.getTechStack());
        resume.setProjectExperience(createDTO.getProjectExperience());
        resume.setInternshipExperience(createDTO.getInternshipExperience());
        resume.setSelfIntroduction(createDTO.getSelfIntroduction());
        resume.setIsDefault(isDefault);
        resumeMapper.insert(resume);
        indexResumeKnowledge(resume);

        return toVO(resume);
    }

    @Override
    public PageResult<ResumeVO> page(Long userId, ResumePageQueryDTO queryDTO) {
        LambdaQueryWrapper<Resume> queryWrapper = new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getIsDefault)
                .orderByDesc(Resume::getUpdateTime);

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(Resume::getTitle, keyword)
                    .or()
                    .like(Resume::getName, keyword)
                    .or()
                    .like(Resume::getSchool, keyword)
                    .or()
                    .like(Resume::getMajor, keyword)
                    .or()
                    .like(Resume::getTechStack, keyword));
        }

        Page<Resume> page = resumeMapper.selectPage(
                new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize()),
                queryWrapper
        );
        List<ResumeVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page, records);
    }

    @Override
    public ResumeVO getDetail(Long userId, Long id) {
        return toVO(getOwnedResume(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO update(Long userId, Long id, ResumeUpdateDTO updateDTO) {
        Resume resume = getOwnedResume(userId, id);
        resume.setTitle(updateDTO.getTitle());
        resume.setName(updateDTO.getName());
        resume.setSchool(updateDTO.getSchool());
        resume.setMajor(updateDTO.getMajor());
        resume.setGrade(updateDTO.getGrade());
        resume.setTechStack(updateDTO.getTechStack());
        resume.setProjectExperience(updateDTO.getProjectExperience());
        resume.setInternshipExperience(updateDTO.getInternshipExperience());
        resume.setSelfIntroduction(updateDTO.getSelfIntroduction());
        resumeMapper.updateById(resume);
        Resume updatedResume = resumeMapper.selectById(id);
        indexResumeKnowledge(updatedResume);
        return toVO(updatedResume);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO setDefault(Long userId, Long id) {
        Resume resume = getOwnedResume(userId, id);
        clearUserDefaultResume(userId);

        resume.setIsDefault(DEFAULT_YES);
        resumeMapper.updateById(resume);
        return toVO(resumeMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwnedResume(userId, id);
        int rows = resumeMapper.delete(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getId, id)
                .eq(Resume::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "简历不存在或无权访问");
        }
    }

    private Resume getOwnedResume(Long userId, Long id) {
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getId, id)
                .eq(Resume::getUserId, userId));
        if (resume == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "简历不存在或无权访问");
        }
        return resume;
    }

    private void clearUserDefaultResume(Long userId) {
        resumeMapper.update(null, new LambdaUpdateWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .eq(Resume::getIsDefault, DEFAULT_YES)
                .set(Resume::getIsDefault, DEFAULT_NO));
    }

    private Integer resolveCreateDefaultValue(Long userId, Integer requestDefaultValue) {
        if (Integer.valueOf(DEFAULT_YES).equals(requestDefaultValue)) {
            return DEFAULT_YES;
        }
        if (Integer.valueOf(DEFAULT_NO).equals(requestDefaultValue)) {
            return DEFAULT_NO;
        }

        Long resumeCount = resumeMapper.selectCount(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId));
        return resumeCount == 0 ? DEFAULT_YES : DEFAULT_NO;
    }

    private ResumeVO toVO(Resume resume) {
        return ResumeVO.builder()
                .id(resume.getId())
                .userId(resume.getUserId())
                .title(resume.getTitle())
                .name(resume.getName())
                .school(resume.getSchool())
                .major(resume.getMajor())
                .grade(resume.getGrade())
                .techStack(resume.getTechStack())
                .projectExperience(resume.getProjectExperience())
                .internshipExperience(resume.getInternshipExperience())
                .selfIntroduction(resume.getSelfIntroduction())
                .isDefault(resume.getIsDefault())
                .createTime(resume.getCreateTime())
                .updateTime(resume.getUpdateTime())
                .build();
    }

    private void indexResumeKnowledge(Resume resume) {
        if (resume == null) {
            return;
        }
        try {
            knowledgeService.indexKnowledge(
                    resume.getUserId(),
                    "RESUME",
                    resume.getId(),
                    defaultIfBlank(resume.getTitle(), defaultIfBlank(resume.getName(), "Resume")),
                    buildResumeKnowledgeContent(resume)
            );
        } catch (Exception e) {
            log.warn("[KnowledgeIndex] resume index failed userId={}, resumeId={}", resume.getUserId(), resume.getId(), e);
        }
    }

    private String buildResumeKnowledgeContent(Resume resume) {
        return "简历名称：" + defaultIfBlank(resume.getTitle(), "") + "\n"
                + "姓名：" + defaultIfBlank(resume.getName(), "") + "\n"
                + "学校：" + defaultIfBlank(resume.getSchool(), "") + "\n"
                + "专业：" + defaultIfBlank(resume.getMajor(), "") + "\n"
                + "年级：" + defaultIfBlank(resume.getGrade(), "") + "\n"
                + "技术栈：" + defaultIfBlank(resume.getTechStack(), "") + "\n"
                + "项目经历：" + defaultIfBlank(resume.getProjectExperience(), "") + "\n"
                + "实习经历：" + defaultIfBlank(resume.getInternshipExperience(), "") + "\n"
                + "自我介绍：" + defaultIfBlank(resume.getSelfIntroduction(), "");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
