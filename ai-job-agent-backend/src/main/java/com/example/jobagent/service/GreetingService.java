package com.example.jobagent.service;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.GreetingGenerateDTO;
import com.example.jobagent.dto.GreetingPageQueryDTO;
import com.example.jobagent.vo.GreetingVO;

public interface GreetingService {

    GreetingVO generate(Long userId, GreetingGenerateDTO generateDTO);

    PageResult<GreetingVO> page(Long userId, GreetingPageQueryDTO queryDTO);

    GreetingVO getDetail(Long userId, Long id);

    void delete(Long userId, Long id);
}
