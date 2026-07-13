package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resume_rewrite_record")
@EqualsAndHashCode(callSuper = true)
public class ResumeRewriteRecord extends BaseEntity {

    private Long userId;

    private Long resumeId;

    private Long jobPostId;

    private String originalProject;

    private String rewrittenProject;

    private String rewriteReason;

    private String resumeVersion;

    private String rawResult;
}
