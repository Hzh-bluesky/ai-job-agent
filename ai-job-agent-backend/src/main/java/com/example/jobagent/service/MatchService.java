package com.example.jobagent.service;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.MatchCreateDTO;
import com.example.jobagent.dto.MatchPageQueryDTO;
import com.example.jobagent.vo.MatchReportVO;

public interface MatchService {

    MatchReportVO create(Long userId, MatchCreateDTO createDTO);

    MatchReportVO getDetail(Long userId, Long id);

    PageResult<MatchReportVO> page(Long userId, MatchPageQueryDTO queryDTO);

    void delete(Long userId, Long id);
}
