package com.example.jobagent.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JobImportResultVO {

    private Integer totalCount;

    private Integer successCount;

    private Integer failureCount;

    private List<JobImportItemVO> items;
}
