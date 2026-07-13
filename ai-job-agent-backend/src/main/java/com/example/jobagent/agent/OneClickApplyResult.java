package com.example.jobagent.agent;

import com.example.jobagent.vo.ApplicationRecordVO;
import com.example.jobagent.vo.GreetingVO;
import com.example.jobagent.vo.InterviewQuestionVO;
import com.example.jobagent.vo.MatchReportVO;
import com.example.jobagent.vo.ResumeRewriteVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OneClickApplyResult {

    private MatchReportVO matchReport;

    private ResumeRewriteVO resumeRewrite;

    private GreetingVO greeting;

    private InterviewQuestionVO interviewQuestions;

    private ApplicationRecordVO applicationRecord;

    private String nextStepSuggestion;
}
