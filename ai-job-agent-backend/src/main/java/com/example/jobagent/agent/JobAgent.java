package com.example.jobagent.agent;

import com.example.jobagent.agent.skill.GreetingGenerateSkill;
import com.example.jobagent.agent.skill.InterviewQuestionSkill;
import com.example.jobagent.agent.skill.JDParseSkill;
import com.example.jobagent.agent.skill.ResumeMatchSkill;
import com.example.jobagent.agent.skill.ResumeRewriteSkill;
import com.example.jobagent.entity.GreetingRecord;
import com.example.jobagent.entity.InterviewQuestionRecord;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.MatchReport;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.entity.ResumeRewriteRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobAgent {

    private final JDParseSkill jdParseSkill;
    private final ResumeMatchSkill resumeMatchSkill;
    private final ResumeRewriteSkill resumeRewriteSkill;
    private final GreetingGenerateSkill greetingGenerateSkill;
    private final InterviewQuestionSkill interviewQuestionSkill;

    public JobAnalysis parseJob(Long userId, String jdText) {
        return jdParseSkill.parse(userId, jdText);
    }

    public MatchReport matchResume(Long userId, Resume resume, JobPost jobPost, JobAnalysis jobAnalysis) {
        return resumeMatchSkill.match(userId, resume, jobPost, jobAnalysis);
    }

    public ResumeRewriteRecord rewriteResume(Long userId,
                                             Resume resume,
                                             JobPost jobPost,
                                             JobAnalysis jobAnalysis,
                                             String projectExperience) {
        return resumeRewriteSkill.rewrite(userId, resume, jobPost, jobAnalysis, projectExperience);
    }

    public GreetingRecord generateGreeting(Long userId, Resume resume, JobPost jobPost, JobAnalysis jobAnalysis) {
        return greetingGenerateSkill.generate(userId, resume, jobPost, jobAnalysis);
    }

    public InterviewQuestionRecord generateInterviewQuestions(Long userId, JobPost jobPost, JobAnalysis jobAnalysis) {
        return interviewQuestionSkill.generate(userId, jobPost, jobAnalysis);
    }
}
