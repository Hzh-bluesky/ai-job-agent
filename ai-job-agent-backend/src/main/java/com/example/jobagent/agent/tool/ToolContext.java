package com.example.jobagent.agent.tool;

import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.vo.ApplicationRecordVO;
import com.example.jobagent.vo.GreetingVO;
import com.example.jobagent.vo.InterviewQuestionVO;
import com.example.jobagent.vo.MatchReportVO;
import com.example.jobagent.vo.ResumeRewriteVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolContext {

    private Long userId;

    private Long resumeId;

    private Long jobPostId;

    private Long jobAnalysisId;

    private Long matchReportId;

    private Long resumeRewriteRecordId;

    private Long greetingRecordId;

    private Long interviewRecordId;

    private Long applicationRecordId;

    private String ragContext;

    private Resume resume;

    private JobPost jobPost;

    private JobAnalysis jobAnalysis;

    private MatchReportVO matchReport;

    private ResumeRewriteVO resumeRewrite;

    private GreetingVO greeting;

    private InterviewQuestionVO interviewQuestions;

    private ApplicationRecordVO applicationRecord;

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();
}
