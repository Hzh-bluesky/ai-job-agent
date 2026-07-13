package com.example.jobagent.service;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.ApplyPlanCreateDTO;
import com.example.jobagent.dto.ApplyPlanPageQueryDTO;
import com.example.jobagent.vo.ApplyPlanVO;

public interface ApplyPlanService {

    ApplyPlanVO generate(Long userId, ApplyPlanCreateDTO createDTO);

    PageResult<ApplyPlanVO> page(Long userId, ApplyPlanPageQueryDTO queryDTO);

    ApplyPlanVO getDetail(Long userId, Long id);
}
