package com.example.jobagent.agent.skill;

import com.example.jobagent.entity.JobAnalysis;

public interface JDParseSkill {

    JobAnalysis parse(Long userId, String jdText);
}
