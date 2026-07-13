package com.example.jobagent.agent.skill.impl;

import com.example.jobagent.agent.rag.RagContextHolder;
import com.example.jobagent.agent.skill.GreetingGenerateSkill;
import com.example.jobagent.ai.LLMRequest;
import com.example.jobagent.ai.LLMResponse;
import com.example.jobagent.ai.LLMService;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.entity.GreetingRecord;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GreetingGenerateSkillImpl implements GreetingGenerateSkill {

    private static final String AGENT_NAME = "JobAgent";
    private static final String SKILL_NAME = "GreetingGenerateSkill";

    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @Override
    public GreetingRecord generate(Long userId, Resume resume, JobPost jobPost, JobAnalysis jobAnalysis) {
        LLMRequest request = LLMRequest.builder()
                .userId(userId)
                .agentName(AGENT_NAME)
                .skillName(SKILL_NAME)
                .prompt(buildPrompt(resume, jobPost, jobAnalysis))
                .variables(Map.of(
                        "resumeId", resume.getId(),
                        "jobPostId", jobPost.getId(),
                        "jobAnalysisId", jobAnalysis.getId()
                ))
                .build();

        LLMResponse response = llmService.chat(request);
        if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || !StringUtils.hasText(response.getContent())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "打招呼话术生成失败");
        }

        return parseResponse(response.getContent());
    }

    private String buildPrompt(Resume resume, JobPost jobPost, JobAnalysis jobAnalysis) {
        String prompt = """
                你是一个面向实习求职者的Boss直聘打招呼话术生成助手。
                请根据用户简历和岗位信息，生成一段适合直接发给招聘者的开场沟通话术。
                必须遵守：
                1. 控制在80到150字；
                2. 语气自然，像真实求职者；
                3. 不要太官方；
                4. 突出用户技能、项目和岗位匹配点；
                5. 不要夸大经历；
                6. 必须只返回JSON，不要输出Markdown，不要输出解释。

                JSON字段包括：greetingText。

                用户简历：
                简历名称：%s
                姓名：%s
                学校：%s
                专业：%s
                年级：%s
                技术栈：%s
                项目经历：%s
                实习经历：%s
                自我介绍：%s

                岗位信息：
                岗位原文：%s
                公司：%s
                岗位：%s
                城市：%s
                薪资：%s
                学历要求：%s
                实习周期：%s
                技术栈要求：%s
                岗位职责：%s
                任职要求：%s
                加分项：%s
                风险点：%s
                """.formatted(
                resume.getTitle(),
                resume.getName(),
                resume.getSchool(),
                resume.getMajor(),
                resume.getGrade(),
                resume.getTechStack(),
                resume.getProjectExperience(),
                resume.getInternshipExperience(),
                resume.getSelfIntroduction(),
                jobPost.getJdText(),
                jobAnalysis.getCompanyName(),
                jobAnalysis.getJobName(),
                jobAnalysis.getCity(),
                jobAnalysis.getSalary(),
                jobAnalysis.getEducation(),
                jobAnalysis.getInternshipCycle(),
                jobAnalysis.getTechStack(),
                jobAnalysis.getResponsibilities(),
                jobAnalysis.getRequirements(),
                jobAnalysis.getBonusPoints(),
                jobAnalysis.getRiskPoints()
        );
        return appendRagContext(prompt);
    }

    private String appendRagContext(String prompt) {
        String ragContext = RagContextHolder.get();
        if (!StringUtils.hasText(ragContext)) {
            return prompt;
        }
        return prompt + """

                RAG Context:
                %s
                Use the RAG context only as user-owned historical reference. Do not fabricate experience.
                """.formatted(ragContext);
    }

    private GreetingRecord parseResponse(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            GreetingRecord record = new GreetingRecord();
            record.setGreetingText(text(root, "greetingText"));
            record.setRawResult(content);
            return record;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI返回内容不是合法JSON，打招呼话术生成失败");
        }
    }

    private String text(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? null : node.asText();
    }
}
