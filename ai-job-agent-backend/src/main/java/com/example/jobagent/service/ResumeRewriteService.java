package com.example.jobagent.service;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.ResumeRewriteDTO;
import com.example.jobagent.dto.ResumeRewritePageQueryDTO;
import com.example.jobagent.vo.ResumeRewriteVO;

public interface ResumeRewriteService {

    ResumeRewriteVO create(Long userId, ResumeRewriteDTO rewriteDTO);

    PageResult<ResumeRewriteVO> page(Long userId, ResumeRewritePageQueryDTO queryDTO);

    ResumeRewriteVO getDetail(Long userId, Long id);

    void delete(Long userId, Long id);
}
