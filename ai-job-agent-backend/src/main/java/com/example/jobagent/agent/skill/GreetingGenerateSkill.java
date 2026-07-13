package com.example.jobagent.agent.skill;

import com.example.jobagent.entity.GreetingRecord;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.Resume;

public interface GreetingGenerateSkill {

    GreetingRecord generate(Long userId, Resume resume, JobPost jobPost, JobAnalysis jobAnalysis);
}
