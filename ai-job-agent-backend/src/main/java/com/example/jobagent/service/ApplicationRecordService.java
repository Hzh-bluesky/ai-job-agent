package com.example.jobagent.service;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.ApplicationCreateDTO;
import com.example.jobagent.dto.ApplicationPageQueryDTO;
import com.example.jobagent.dto.ApplicationStatusUpdateDTO;
import com.example.jobagent.dto.ApplicationUpdateDTO;
import com.example.jobagent.vo.ApplicationRecordVO;

public interface ApplicationRecordService {

    ApplicationRecordVO create(Long userId, ApplicationCreateDTO createDTO);

    PageResult<ApplicationRecordVO> page(Long userId, ApplicationPageQueryDTO queryDTO);

    ApplicationRecordVO getDetail(Long userId, Long id);

    ApplicationRecordVO update(Long userId, Long id, ApplicationUpdateDTO updateDTO);

    ApplicationRecordVO updateStatus(Long userId, Long id, ApplicationStatusUpdateDTO statusUpdateDTO);

    void delete(Long userId, Long id);
}
