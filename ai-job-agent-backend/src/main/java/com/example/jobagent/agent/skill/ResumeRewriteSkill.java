package com.example.jobagent.agent.skill;

import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.entity.ResumeRewriteRecord;

public interface ResumeRewriteSkill {

    ResumeRewriteRecord rewrite(Long userId,
                                Resume resume,
                                JobPost jobPost,
                                JobAnalysis jobAnalysis,
                                String projectExperience);
}
