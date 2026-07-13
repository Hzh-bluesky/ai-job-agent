package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("apply_plan_record")
@EqualsAndHashCode(callSuper = true)
public class ApplyPlanRecord extends BaseEntity {

    private Long userId;

    private Long resumeId;

    private Long jobPostId;

    private Long matchReportId;

    private Long resumeRewriteRecordId;

    private Long greetingRecordId;

    private Long interviewQuestionRecordId;

    private Long applicationRecordId;

    private String status;

    private String nextStepSuggestion;

    private String errorMessage;
}
