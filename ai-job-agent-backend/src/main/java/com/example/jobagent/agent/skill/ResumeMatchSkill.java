package com.example.jobagent.agent.skill;

import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.MatchReport;
import com.example.jobagent.entity.Resume;

public interface ResumeMatchSkill {

    MatchReport match(Long userId, Resume resume, JobPost jobPost, JobAnalysis jobAnalysis);
}
