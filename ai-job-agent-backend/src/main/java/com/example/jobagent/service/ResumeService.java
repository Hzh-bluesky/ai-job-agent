package com.example.jobagent.service;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.ResumeCreateDTO;
import com.example.jobagent.dto.ResumePageQueryDTO;
import com.example.jobagent.dto.ResumeUpdateDTO;
import com.example.jobagent.vo.ResumeVO;

public interface ResumeService {

    ResumeVO create(Long userId, ResumeCreateDTO createDTO);

    PageResult<ResumeVO> page(Long userId, ResumePageQueryDTO queryDTO);

    ResumeVO getDetail(Long userId, Long id);

    ResumeVO update(Long userId, Long id, ResumeUpdateDTO updateDTO);

    ResumeVO setDefault(Long userId, Long id);

    void delete(Long userId, Long id);
}
