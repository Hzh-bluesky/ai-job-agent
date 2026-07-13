package com.example.jobagent.agent.skill.impl;

import com.example.jobagent.agent.rag.RagContextHolder;
import com.example.jobagent.agent.skill.ResumeMatchSkill;
import com.example.jobagent.ai.LLMRequest;
import com.example.jobagent.ai.LLMResponse;
import com.example.jobagent.ai.LLMService;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.MatchReport;
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
public class ResumeMatchSkillImpl implements ResumeMatchSkill {

    private static final String AGENT_NAME = "JobAgent";
    private static final String SKILL_NAME = "ResumeMatchSkill";

    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @Override
    public MatchReport match(Long userId, Resume resume, JobPost jobPost, JobAnalysis jobAnalysis) {
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "简历匹配失败");
        }

        return parseResponse(response.getContent());
    }

    private String buildPrompt(Resume resume, JobPost jobPost, JobAnalysis jobAnalysis) {
        String prompt = """
                你是一个实习求职简历匹配助手。请根据用户简历和岗位分析结果，生成结构化匹配报告。
                必须只返回JSON，不要输出Markdown，不要输出解释。
                JSON字段包括：
                overallScore, techScore, projectScore, educationScore,
                advantageAnalysis, weaknessAnalysis, suggestion, isRecommended。
                分数必须是0到100之间的整数，isRecommended用0或1。

                简历名称：%s
                姓名：%s
                学校：%s
                专业：%s
                年级：%s
                技术栈：%s
                项目经历：%s
                实习经历：%s
                自我介绍：%s

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

    private MatchReport parseResponse(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            MatchReport report = new MatchReport();
            report.setOverallScore(score(root, "overallScore"));
            report.setTechScore(score(root, "techScore"));
            report.setProjectScore(score(root, "projectScore"));
            report.setEducationScore(score(root, "educationScore"));
            report.setAdvantageAnalysis(text(root, "advantageAnalysis"));
            report.setWeaknessAnalysis(text(root, "weaknessAnalysis"));
            report.setSuggestion(text(root, "suggestion"));
            report.setIsRecommended(flag(root, "isRecommended"));
            report.setRawResult(content);
            return report;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI返回内容不是合法JSON，简历匹配失败");
        }
    }

    private Integer score(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        int value = node == null || !node.canConvertToInt() ? 0 : node.asInt();
        return Math.max(0, Math.min(100, value));
    }

    private Integer flag(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        int value = node == null || !node.canConvertToInt() ? 0 : node.asInt();
        return value == 1 ? 1 : 0;
    }

    private String text(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? null : node.asText();
    }
}
