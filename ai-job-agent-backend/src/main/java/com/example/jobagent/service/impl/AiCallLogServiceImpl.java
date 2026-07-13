package com.example.jobagent.service.impl;

import com.example.jobagent.entity.AiCallLog;
import com.example.jobagent.mapper.AiCallLogMapper;
import com.example.jobagent.service.AiCallLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiCallLogServiceImpl implements AiCallLogService {

    private final AiCallLogMapper aiCallLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(AiCallLog log) {
        aiCallLogMapper.insert(log);
    }
}
