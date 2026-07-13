package com.example.jobagent.agent.skill.impl;

import com.example.jobagent.agent.rag.RagContextHolder;
import com.example.jobagent.agent.skill.InterviewQuestionSkill;
import com.example.jobagent.ai.LLMRequest;
import com.example.jobagent.ai.LLMResponse;
import com.example.jobagent.ai.LLMService;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.entity.InterviewQuestionRecord;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class InterviewQuestionSkillImpl implements InterviewQuestionSkill {

    private static final String AGENT_NAME = "JobAgent";
    private static final String SKILL_NAME = "InterviewQuestionSkill";

    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewQuestionRecord generate(Long userId, JobPost jobPost, JobAnalysis jobAnalysis) {
        LLMRequest request = LLMRequest.builder()
                .userId(userId)
                .agentName(AGENT_NAME)
                .skillName(SKILL_NAME)
                .prompt(buildPrompt(jobPost, jobAnalysis))
                .variables(Map.of(
                        "jobPostId", jobPost.getId(),
                        "jobAnalysisId", jobAnalysis.getId()
                ))
                .build();

        LLMResponse response = llmService.chat(request);
        if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || !StringUtils.hasText(response.getContent())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "面试题生成失败");
        }

        return parseResponse(response.getContent());
    }

    private String buildPrompt(JobPost jobPost, JobAnalysis jobAnalysis) {
        String prompt = """
                你是一个面向实习求职者的面试准备助手。
                请根据岗位JD和岗位分析结果，生成结构化面试准备内容。
                必须遵守：
                1. technicalQuestions生成10道技术面试题；
                2. projectQuestions生成5道项目追问题；
                3. hrQuestions生成5道HR常见问题；
                4. 每道题都必须有question和answerIdea；
                5. 问题要结合AI Agent、Java、Spring Boot、MySQL、Vue3、JWT、MyBatis-Plus、LLM、Skill、MockAIService、用户数据隔离等关键词；
                6. 必须只返回JSON，不要输出Markdown，不要输出解释。

                JSON字段包括：technicalQuestions, projectQuestions, hrQuestions。

                岗位原文：
                %s

                岗位分析：
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

    private InterviewQuestionRecord parseResponse(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            InterviewQuestionRecord record = new InterviewQuestionRecord();
            record.setTechnicalQuestions(jsonArray(root, "technicalQuestions"));
            record.setProjectQuestions(jsonArray(root, "projectQuestions"));
            record.setHrQuestions(jsonArray(root, "hrQuestions"));
            record.setRawResult(content);
            return record;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI返回内容不是合法JSON，面试题生成失败");
        }
    }

    private String jsonArray(JsonNode root, String fieldName) throws Exception {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isArray()) {
            return "[]";
        }
        return objectMapper.writeValueAsString(node);
    }
}
