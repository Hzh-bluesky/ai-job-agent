package com.example.jobagent.ai.eval;

import com.example.jobagent.ai.LLMRequest;
import com.example.jobagent.ai.LLMResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiOutputEvaluator {

    private final ObjectMapper objectMapper;

    public EvaluationResult evaluate(LLMRequest request, LLMResponse response) {
        List<String> missingFields = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        int score = 100;

        if (response == null) {
            return failed("LLM response is null", "请重新调用模型并返回合法JSON");
        }
        if (!Boolean.TRUE.equals(response.getSuccess())) {
            return EvaluationResult.builder()
                    .passed(false)
                    .score(0)
                    .missingFields(List.of())
                    .issues(List.of(defaultIfBlank(response.getErrorMessage(), "LLM call failed")))
                    .suggestion("检查模型调用错误后重试")
                    .build();
        }
        if (!StringUtils.hasText(response.getContent())) {
            return failed("LLM content is empty", "请返回非空JSON对象");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getContent());
        } catch (Exception e) {
            return failed("JSON parse failed: " + e.getMessage(), "请只返回合法JSON，不要包含Markdown或解释文字");
        }
        if (root == null || !root.isObject()) {
            return failed("JSON root must be an object", "请返回JSON对象");
        }

        String skillName = request == null ? "" : request.getSkillName();
        if ("JDParseSkill".equals(skillName)) {
            score -= evaluateJDParse(root, missingFields, issues);
        } else if ("ResumeMatchSkill".equals(skillName)) {
            score -= evaluateResumeMatch(root, missingFields, issues);
        } else if ("ResumeRewriteSkill".equals(skillName)) {
            score -= evaluateResumeRewrite(root, missingFields, issues);
        } else if ("GreetingGenerateSkill".equals(skillName)) {
            score -= evaluateGreeting(root, missingFields, issues);
        } else if ("InterviewQuestionSkill".equals(skillName)) {
            score -= evaluateInterview(root, missingFields, issues);
        } else {
            score -= evaluateGeneric(root, issues);
        }

        score -= evaluatePlaceholderText(root, issues);
        int normalizedScore = Math.max(0, Math.min(100, score));
        boolean passed = missingFields.isEmpty() && normalizedScore >= 70;
        return EvaluationResult.builder()
                .passed(passed)
                .score(normalizedScore)
                .missingFields(missingFields)
                .issues(issues)
                .suggestion(buildSuggestion(missingFields, issues))
                .build();
    }

    private int evaluateJDParse(JsonNode root, List<String> missingFields, List<String> issues) {
        int penalty = 0;
        penalty += requireText(root, "jobName", missingFields, issues, "岗位名称不能为空");
        penalty += requireText(root, "techStack", missingFields, issues, "技术栈不能为空");
        penalty += requireText(root, "requirements", missingFields, issues, "任职要求不能为空");
        penalty += softRequireText(root, "responsibilities", issues, "岗位职责为空");
        if (text(root, "techStack").length() < 4) {
            issues.add("技术栈内容过短");
            penalty += 15;
        }
        return penalty;
    }

    private int evaluateResumeMatch(JsonNode root, List<String> missingFields, List<String> issues) {
        int penalty = 0;
        penalty += requireScore(root, "overallScore", missingFields, issues);
        penalty += requireScore(root, "techScore", missingFields, issues);
        penalty += requireScore(root, "projectScore", missingFields, issues);
        penalty += requireScore(root, "educationScore", missingFields, issues);
        penalty += requireText(root, "advantageAnalysis", missingFields, issues, "优势分析不能为空");
        penalty += requireText(root, "weaknessAnalysis", missingFields, issues, "不足分析不能为空");
        penalty += requireText(root, "suggestion", missingFields, issues, "补强建议不能为空");
        if (!root.has("isRecommended")) {
            missingFields.add("isRecommended");
            penalty += 20;
        }
        return penalty;
    }

    private int evaluateResumeRewrite(JsonNode root, List<String> missingFields, List<String> issues) {
        int penalty = 0;
        penalty += requireText(root, "rewrittenProject", missingFields, issues, "优化后项目描述不能为空");
        penalty += requireText(root, "rewriteReason", missingFields, issues, "优化理由不能为空");
        penalty += requireText(root, "resumeVersion", missingFields, issues, "简历版本不能为空");
        if (text(root, "rewrittenProject").length() < 30) {
            issues.add("优化后项目描述过短");
            penalty += 15;
        }
        return penalty;
    }

    private int evaluateGreeting(JsonNode root, List<String> missingFields, List<String> issues) {
        int penalty = requireText(root, "greetingText", missingFields, issues, "打招呼话术不能为空");
        String greetingText = text(root, "greetingText");
        int length = greetingText.length();
        if (length > 0 && (length < 40 || length > 220)) {
            issues.add("打招呼话术长度不合理，建议80到150字");
            penalty += 20;
        }
        return penalty;
    }

    private int evaluateInterview(JsonNode root, List<String> missingFields, List<String> issues) {
        int penalty = 0;
        penalty += requireQuestionArray(root, "technicalQuestions", 10, missingFields, issues);
        penalty += requireQuestionArray(root, "projectQuestions", 5, missingFields, issues);
        penalty += requireQuestionArray(root, "hrQuestions", 5, missingFields, issues);
        return penalty;
    }

    private int evaluateGeneric(JsonNode root, List<String> issues) {
        if (root.size() == 0) {
            issues.add("JSON对象为空");
            return 40;
        }
        return 0;
    }

    private int requireQuestionArray(JsonNode root,
                                     String fieldName,
                                     int expectedCount,
                                     List<String> missingFields,
                                     List<String> issues) {
        JsonNode array = root.get(fieldName);
        if (array == null || !array.isArray()) {
            missingFields.add(fieldName);
            return 25;
        }
        int penalty = 0;
        if (array.size() < expectedCount) {
            issues.add(fieldName + "数量不足，期望" + expectedCount + "道，实际" + array.size() + "道");
            penalty += Math.min(25, (expectedCount - array.size()) * 4);
        }
        for (int i = 0; i < array.size(); i++) {
            JsonNode item = array.get(i);
            if (!StringUtils.hasText(text(item, "question")) || !StringUtils.hasText(text(item, "answerIdea"))) {
                issues.add(fieldName + "第" + (i + 1) + "题缺少question或answerIdea");
                penalty += 5;
            }
        }
        return penalty;
    }

    private int requireText(JsonNode root, String fieldName, List<String> missingFields, List<String> issues, String issue) {
        if (!StringUtils.hasText(text(root, fieldName))) {
            missingFields.add(fieldName);
            issues.add(issue);
            return 25;
        }
        return 0;
    }

    private int softRequireText(JsonNode root, String fieldName, List<String> issues, String issue) {
        if (!StringUtils.hasText(text(root, fieldName))) {
            issues.add(issue);
            return 10;
        }
        return 0;
    }

    private int requireScore(JsonNode root, String fieldName, List<String> missingFields, List<String> issues) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.canConvertToInt()) {
            missingFields.add(fieldName);
            return 20;
        }
        int score = node.asInt();
        if (score < 0 || score > 100) {
            issues.add(fieldName + "不在0到100范围内");
            return 20;
        }
        return 0;
    }

    private int evaluatePlaceholderText(JsonNode root, List<String> issues) {
        String text = root.toString().toLowerCase();
        String[] placeholders = {"todo", "待补充", "待填写", "xxx", "n/a", "占位"};
        for (String placeholder : placeholders) {
            if (text.contains(placeholder)) {
                issues.add("包含明显占位文本：" + placeholder);
                return 20;
            }
        }
        return 0;
    }

    private String text(JsonNode root, String fieldName) {
        if (root == null) {
            return "";
        }
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText().trim();
        }
        return node.toString().trim();
    }

    private EvaluationResult failed(String issue, String suggestion) {
        return EvaluationResult.builder()
                .passed(false)
                .score(0)
                .missingFields(List.of())
                .issues(List.of(issue))
                .suggestion(suggestion)
                .build();
    }

    private String buildSuggestion(List<String> missingFields, List<String> issues) {
        List<String> parts = new ArrayList<>();
        if (!missingFields.isEmpty()) {
            parts.add("补齐字段：" + String.join(",", missingFields));
        }
        if (!issues.isEmpty()) {
            parts.add("修复问题：" + String.join("；", issues));
        }
        if (parts.isEmpty()) {
            return "输出质量合格";
        }
        return String.join("；", parts);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
