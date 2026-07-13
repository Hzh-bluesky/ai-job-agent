package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "One click apply plan response")
public class ApplyPlanVO {

    private Long id;

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

    private MatchReportVO matchReport;

    private ResumeRewriteVO resumeRewrite;

    private GreetingVO greeting;

    private InterviewQuestionVO interviewQuestions;

    private ApplicationRecordVO applicationRecord;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
