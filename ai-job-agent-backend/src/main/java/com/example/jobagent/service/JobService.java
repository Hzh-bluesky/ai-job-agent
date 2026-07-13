package com.example.jobagent.service;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.JobAnalyzeDTO;
import com.example.jobagent.dto.JobPageQueryDTO;
import com.example.jobagent.vo.JobAnalysisVO;
import com.example.jobagent.vo.JobImportResultVO;
import com.example.jobagent.vo.JobPostVO;
import org.springframework.web.multipart.MultipartFile;

public interface JobService {

    JobAnalysisVO analyze(Long userId, JobAnalyzeDTO analyzeDTO);

    JobImportResultVO importJobs(Long userId, MultipartFile file);

    PageResult<JobPostVO> page(Long userId, JobPageQueryDTO queryDTO);

    JobPostVO getDetail(Long userId, Long id);

    void delete(Long userId, Long id);
}
