package com.example.jobagent.service;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.InterviewGenerateDTO;
import com.example.jobagent.dto.InterviewPageQueryDTO;
import com.example.jobagent.vo.InterviewQuestionVO;

public interface InterviewService {

    InterviewQuestionVO generate(Long userId, InterviewGenerateDTO generateDTO);

    PageResult<InterviewQuestionVO> page(Long userId, InterviewPageQueryDTO queryDTO);

    InterviewQuestionVO getDetail(Long userId, Long id);

    void delete(Long userId, Long id);
}
