package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "简历项目优化记录")
public class ResumeRewriteVO {

    private Long id;

    private Long userId;

    private Long resumeId;

    private Long jobPostId;

    private String originalProject;

    private String rewrittenProject;

    private String rewriteReason;

    private String resumeVersion;

    private String rawResult;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
