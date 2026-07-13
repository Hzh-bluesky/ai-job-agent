package com.example.jobagent.agent.skill;

import com.example.jobagent.entity.InterviewQuestionRecord;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;

public interface InterviewQuestionSkill {

    InterviewQuestionRecord generate(Long userId, JobPost jobPost, JobAnalysis jobAnalysis);
}
