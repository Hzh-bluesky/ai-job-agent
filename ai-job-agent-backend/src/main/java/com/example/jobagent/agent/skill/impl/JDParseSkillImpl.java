package com.example.jobagent.agent.skill.impl;

import com.example.jobagent.agent.skill.JDParseSkill;
import com.example.jobagent.ai.LLMRequest;
import com.example.jobagent.ai.LLMResponse;
import com.example.jobagent.ai.LLMService;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JDParseSkillImpl implements JDParseSkill {

    private static final String AGENT_NAME = "JobAgent";
    private static final String SKILL_NAME = "JDParseSkill";

    private static final String[][] TECH_KEYWORDS = {
            {"Spring Boot", "spring boot", "springboot"},
            {"Spring Cloud", "spring cloud", "springcloud"},
            {"Prompt Engineering", "prompt engineering", "prompt", "提示词"},
            {"AI Agent", "ai agent", "agent"},
            {"大模型 API", "大模型api", "大模型 api", "llm api", "大模型接口", "大模型调用"},
            {"MyBatis-Plus", "mybatis-plus", "mybatis plus"},
            {"Vue3", "vue3", "vue 3"},
            {"MySQL", "mysql"},
            {"LLM", "llm", "大模型"},
            {"MCP", "mcp"},
            {"Java", "java"},
            {"MyBatis", "mybatis"},
            {"Redis", "redis"},
            {"Vue", "vue"},
            {"React", "react"},
            {"JavaScript", "javascript", "js"},
            {"TypeScript", "typescript", "ts"},
            {"HTML", "html"},
            {"CSS", "css"},
            {"JWT", "jwt"},
            {"RESTful API", "restful", "rest api", "api接口"},
            {"Docker", "docker"},
            {"Linux", "linux"},
            {"Git", "git"},
            {"Maven", "maven"},
            {"Nginx", "nginx"},
            {"RabbitMQ", "rabbitmq"},
            {"Kafka", "kafka"},
            {"Elasticsearch", "elasticsearch", "elastic search"},
            {"Python", "python"},
            {"Skill", "skill"},
            {"RAG", "rag"}
    };

    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @Override
    public JobAnalysis parse(Long userId, String jdText) {
        LLMRequest request = LLMRequest.builder()
                .userId(userId)
                .agentName(AGENT_NAME)
                .skillName(SKILL_NAME)
                .prompt(buildPrompt(jdText))
                .variables(Map.of("jdText", jdText))
                .build();

        LLMResponse response = llmService.chat(request);
        if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || !StringUtils.hasText(response.getContent())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "岗位JD解析失败");
        }

        return parseResponse(response.getContent(), jdText);
    }

    private String buildPrompt(String jdText) {
        return """
                你是 AI 求职投递助手 Agent 中的 JDParseSkill，负责把实习/初级岗位 JD 解析成稳定结构化 JSON。

                你必须严格遵守：
                1. 只返回一个合法 JSON 对象，不要 Markdown，不要解释，不要代码块。
                2. JSON 必须包含且只包含这些字段：
                   companyName, jobName, city, salary, education, internshipCycle,
                   techStack, responsibilities, requirements, bonusPoints, riskPoints
                3. 所有字段值都使用字符串；没有明确内容时不要返回 null，可以返回空字符串或基于 JD 的保守推断。
                4. techStack 必须从“岗位要求 / 任职要求 / 技术要求 / 加分项 / 岗位描述”中兜底抽取。
                   技术栈优先保留组合词，不要拆碎：Spring Boot、Prompt Engineering、AI Agent、大模型 API、MyBatis-Plus、Vue3、MySQL、LLM、MCP。
                   如果出现 Spring Boot，不要再单独输出 Spring 或 Boot；出现 Prompt Engineering，不要再单独输出 Prompt 或 Engineering；出现 AI Agent，不要再单独输出 AI 或 Agent；出现 MyBatis-Plus，不要再单独输出 MyBatis；出现 Vue3，不要再单独输出 Vue。
                5. internshipCycle 只有 JD 明确出现“实习周期、实习时间、实习时长、每周、几个月、至少、到岗、可实习”等信息时才填写。
                   “AI Agent应用开发实习生”“Java后端实习生”这类岗位名称不是实习周期，不能填入 internshipCycle。
                   如果没有明确周期信息，internshipCycle 返回空字符串。
                6. responsibilities 如果 JD 没有明确职责，请根据岗位名称、技术栈和业务内容生成 2-4 条合理职责，不能留空。
                7. bonusPoints 只要出现“优先、加分、熟悉、了解、有经验、开源、项目经验、实习经验”等表达，就必须提取为加分项。
                8. riskPoints 如果 JD 信息缺失，例如公司、城市、薪资、学历、实习周期、职责、任职要求不完整，要写出风险点。
                9. 对短 JD 也要尽量抽取，不要因为 JD 简短就返回空字段。

                返回 JSON 示例：
                {
                  "companyName": "",
                  "jobName": "AI Agent 应用开发实习生",
                  "city": "",
                  "salary": "",
                  "education": "本科及以上",
                  "internshipCycle": "",
                  "techStack": "Java, Spring Boot, Vue3, MySQL, AI Agent, Skill, MCP",
                  "responsibilities": "参与 AI Agent 应用后端接口开发；协助实现 Agent 与 Skill 调用链；维护岗位分析、简历匹配等业务模块。",
                  "requirements": "熟悉 Java、Spring Boot、MySQL，了解 Vue3 和 AI Agent 基本开发流程。",
                  "bonusPoints": "了解 MCP、LLM 调用、Prompt 设计或有相关项目经验优先。",
                  "riskPoints": "JD 未明确薪资、城市、实习周期，建议投递前进一步确认。"
                }

                岗位 JD：
                %s
                """.formatted(jdText);
    }

    private JobAnalysis parseResponse(String content, String jdText) {
        JobAnalysis analysis;
        try {
            String json = extractJsonObject(content);
            JsonNode root = objectMapper.readTree(json);
            analysis = new JobAnalysis();
            analysis.setCompanyName(text(root, "companyName"));
            analysis.setJobName(text(root, "jobName"));
            analysis.setCity(text(root, "city"));
            analysis.setSalary(text(root, "salary"));
            analysis.setEducation(text(root, "education"));
            analysis.setInternshipCycle(text(root, "internshipCycle"));
            analysis.setTechStack(text(root, "techStack"));
            analysis.setResponsibilities(text(root, "responsibilities"));
            analysis.setRequirements(text(root, "requirements"));
            analysis.setBonusPoints(text(root, "bonusPoints"));
            analysis.setRiskPoints(text(root, "riskPoints"));
        } catch (Exception ex) {
            // 模型偶尔会返回非标准 JSON，这里基于原始 JD 做保守兜底，避免整段内容粗暴塞进 requirements。
            analysis = buildFallbackAnalysis(jdText);
        }

        enrichByOriginalJd(analysis, jdText);
        analysis.setRawResult(content);
        return analysis;
    }

    private JobAnalysis buildFallbackAnalysis(String jdText) {
        JobAnalysis analysis = new JobAnalysis();
        analysis.setJobName(extractJobName(jdText));
        analysis.setCompanyName(extractCompanyName(jdText));
        analysis.setCity(extractCity(jdText));
        analysis.setSalary(extractSalary(jdText));
        analysis.setEducation(extractEducation(jdText));
        analysis.setInternshipCycle(extractInternshipCycle(jdText));
        analysis.setTechStack(join(extractTechStack(jdText)));
        analysis.setResponsibilities(extractResponsibilities(jdText, analysis));
        analysis.setRequirements(extractByKeywords(jdText, List.of("要求", "任职", "熟悉", "掌握", "了解", "经验", "本科", "大专", "计算机")));
        analysis.setBonusPoints(extractBonusPoints(jdText));
        analysis.setRiskPoints(buildRiskPoints(analysis, jdText));
        return analysis;
    }

    private void enrichByOriginalJd(JobAnalysis analysis, String jdText) {
        List<String> techStack = extractTechStack(jdText);
        analysis.setTechStack(mergeTextList(analysis.getTechStack(), techStack));

        if (!StringUtils.hasText(analysis.getJobName())) {
            analysis.setJobName(extractJobName(jdText));
        }
        if (!StringUtils.hasText(analysis.getCompanyName())) {
            analysis.setCompanyName(extractCompanyName(jdText));
        }
        if (!StringUtils.hasText(analysis.getCity())) {
            analysis.setCity(extractCity(jdText));
        }
        if (!StringUtils.hasText(analysis.getSalary())) {
            analysis.setSalary(extractSalary(jdText));
        }
        if (!StringUtils.hasText(analysis.getEducation())) {
            analysis.setEducation(extractEducation(jdText));
        }
        if (!hasExplicitInternshipCycle(jdText)) {
            analysis.setInternshipCycle("");
        } else if (!StringUtils.hasText(analysis.getInternshipCycle())) {
            analysis.setInternshipCycle(extractInternshipCycle(jdText));
        }
        if (!StringUtils.hasText(analysis.getRequirements())) {
            analysis.setRequirements(extractByKeywords(jdText, List.of("要求", "任职", "熟悉", "掌握", "了解", "经验", "本科", "大专", "计算机")));
        }
        if (!StringUtils.hasText(analysis.getResponsibilities())) {
            analysis.setResponsibilities(extractResponsibilities(jdText, analysis));
        }
        if (!StringUtils.hasText(analysis.getBonusPoints())) {
            analysis.setBonusPoints(extractBonusPoints(jdText));
        }
        String generatedRiskPoints = buildRiskPoints(analysis, jdText);
        if (!StringUtils.hasText(analysis.getRiskPoints())) {
            analysis.setRiskPoints(generatedRiskPoints);
        } else if (shouldAppendRiskPoints(analysis.getRiskPoints(), generatedRiskPoints)) {
            analysis.setRiskPoints(analysis.getRiskPoints() + "；" + generatedRiskPoints);
        }
    }

    private String extractJsonObject(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                text = text.substring(firstLineEnd + 1, lastFence).trim();
            }
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private List<String> extractTechStack(String jdText) {
        String source = safe(jdText).toLowerCase();
        Set<String> result = new LinkedHashSet<>();
        for (String[] keyword : TECH_KEYWORDS) {
            for (String alias : keyword) {
                if (source.contains(alias.toLowerCase())) {
                    result.add(keyword[0]);
                    break;
                }
            }
        }
        return normalizeTechStack(result);
    }

    private String buildResponsibilities(JobAnalysis analysis) {
        String jobName = StringUtils.hasText(analysis.getJobName()) ? analysis.getJobName() : "目标岗位";
        String techStack = StringUtils.hasText(analysis.getTechStack()) ? analysis.getTechStack() : "岗位相关技术栈";
        return "参与" + jobName + "相关需求分析与功能开发；基于" + techStack + "完成业务模块实现和接口联调；协助维护系统稳定性并持续优化项目交付质量。";
    }

    private String extractResponsibilities(String jdText, JobAnalysis analysis) {
        List<String> matched = new ArrayList<>();
        for (String line : splitLines(jdText)) {
            boolean explicitDuty = containsAny(line, List.of("职责", "负责", "参与", "协助", "配合"));
            boolean actionDuty = containsAny(line, List.of("开发", "设计", "实现", "维护", "优化", "搭建", "落地"));
            boolean likelyRequirement = containsAny(line, List.of("要求", "任职", "熟悉", "掌握", "了解", "优先", "加分"));
            if ((explicitDuty || actionDuty) && (!likelyRequirement || explicitDuty)) {
                matched.add(line);
            }
        }
        String responsibilities = limitAndJoin(matched, 5);
        return StringUtils.hasText(responsibilities) ? responsibilities : buildResponsibilities(analysis);
    }

    private String extractBonusPoints(String jdText) {
        String result = extractByKeywords(jdText, List.of("优先", "加分", "熟悉", "了解", "有经验", "项目经验", "实习经验", "开源", "MCP", "Agent", "LLM", "RAG", "Prompt"));
        return StringUtils.hasText(result) ? result : "";
    }

    private String buildRiskPoints(JobAnalysis analysis, String jdText) {
        List<String> risks = new ArrayList<>();
        if (!StringUtils.hasText(analysis.getCompanyName())) {
            risks.add("JD未明确公司名称");
        }
        if (!StringUtils.hasText(analysis.getCity())) {
            risks.add("JD未明确工作城市");
        }
        if (!StringUtils.hasText(analysis.getSalary())) {
            risks.add("JD未明确薪资范围");
        }
        if (!StringUtils.hasText(analysis.getEducation())) {
            risks.add("JD未明确学历要求");
        }
        if (!StringUtils.hasText(analysis.getInternshipCycle())) {
            risks.add("JD未明确实习周期或到岗时间");
        }
        if (!StringUtils.hasText(analysis.getResponsibilities()) || safe(jdText).length() < 80) {
            risks.add("JD内容较短，岗位职责可能不完整，建议投递前进一步确认具体工作内容");
        }
        return risks.isEmpty() ? "暂无明显风险点，建议继续结合公司背景和沟通反馈判断。" : String.join("；", risks) + "。";
    }

    private boolean shouldAppendRiskPoints(String existingRiskPoints, String generatedRiskPoints) {
        if (!StringUtils.hasText(generatedRiskPoints) || generatedRiskPoints.startsWith("暂无明显风险点")) {
            return false;
        }
        String existing = safe(existingRiskPoints);
        if (existing.contains("暂无") || existing.contains("无明显") || existing.contains("无风险")) {
            return true;
        }
        String signature = generatedRiskPoints.substring(0, Math.min(12, generatedRiskPoints.length()));
        return !existing.contains(signature);
    }

    private String extractByKeywords(String jdText, List<String> keywords) {
        List<String> lines = splitLines(jdText);
        List<String> matched = new ArrayList<>();
        for (String line : lines) {
            boolean hit = containsAny(line, keywords);
            if (hit && line.length() >= 2) {
                matched.add(line);
            }
        }
        return limitAndJoin(matched, 5);
    }

    private boolean containsAny(String text, List<String> keywords) {
        String lowerText = safe(text).toLowerCase();
        return keywords.stream().anyMatch(keyword -> lowerText.contains(keyword.toLowerCase()));
    }

    private String extractJobName(String jdText) {
        return extractValueByLabels(jdText, List.of("岗位名称", "职位名称", "招聘岗位", "岗位", "职位"));
    }

    private String extractCompanyName(String jdText) {
        return extractValueByLabels(jdText, List.of("公司名称", "公司", "企业"));
    }

    private String extractCity(String jdText) {
        String value = extractValueByLabels(jdText, List.of("工作城市", "工作地点", "地点", "城市"));
        if (StringUtils.hasText(value)) {
            return value;
        }
        List<String> cities = List.of("北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "南京", "苏州", "西安", "重庆", "天津", "长沙", "厦门", "合肥", "郑州", "远程");
        return cities.stream().filter(city -> safe(jdText).contains(city)).findFirst().orElse("");
    }

    private String extractSalary(String jdText) {
        String value = extractValueByLabels(jdText, List.of("薪资", "薪酬", "待遇", "日薪"));
        if (StringUtils.hasText(value)) {
            return value;
        }
        for (String line : splitLines(jdText)) {
            if (line.matches(".*(\\d+\\s*[kK]-\\s*\\d+\\s*[kK]|\\d+\\s*元/天|\\d+-\\d+/天|\\d+\\s*薪|面议).*")) {
                return line;
            }
        }
        return "";
    }

    private String extractEducation(String jdText) {
        String value = extractValueByLabels(jdText, List.of("学历要求", "学历"));
        if (StringUtils.hasText(value)) {
            return value;
        }
        List<String> educations = List.of("本科及以上", "本科", "硕士", "研究生", "大专及以上", "大专", "不限");
        return educations.stream().filter(education -> safe(jdText).contains(education)).findFirst().orElse("");
    }

    private String extractInternshipCycle(String jdText) {
        String value = extractValueByLabels(jdText, List.of("实习周期", "实习时间", "实习时长", "到岗时间", "每周"));
        if (StringUtils.hasText(value)) {
            return value;
        }
        List<String> matched = new ArrayList<>();
        for (String line : splitLines(jdText)) {
            if (isInternshipCycleLine(line)) {
                matched.add(line);
            }
        }
        return limitAndJoin(matched, 3);
    }

    private boolean hasExplicitInternshipCycle(String jdText) {
        return splitLines(jdText).stream().anyMatch(this::isInternshipCycleLine);
    }

    private boolean isInternshipCycleLine(String line) {
        String text = safe(line);
        if (!StringUtils.hasText(text)) {
            return false;
        }
        if (text.matches(".*(实习周期|实习时间|实习时长|到岗时间|每周|可实习|到岗|尽快到岗).*")) {
            return true;
        }
        if (text.matches(".*(至少|不少于|以上).*") && text.matches(".*(实习|月|周|天|到岗).*")) {
            return true;
        }
        return text.matches(".*(\\d+\\s*(个)?月|\\d+\\s*周|\\d+\\s*天|\\d+\\s*个月以上|[一二三四五六七八九十两]+\\s*(个)?月).*");
    }

    private String extractValueByLabels(String jdText, List<String> labels) {
        for (String line : splitLines(jdText)) {
            for (String label : labels) {
                if (line.startsWith(label + "：") || line.startsWith(label + ":")) {
                    return cleanValue(line.substring(label.length() + 1));
                }
            }
        }
        return "";
    }

    private List<String> splitLines(String text) {
        String normalized = safe(text)
                .replace("；", "\n")
                .replace("。", "\n")
                .replace("，", "\n")
                .replace(",", "\n")
                .replace("、", "\n")
                .replace("；", "\n")
                .replace(";", "\n");
        String[] rawLines = normalized.split("\\r?\\n");
        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            String line = cleanValue(rawLine);
            if (StringUtils.hasText(line)) {
                lines.add(line);
            }
        }
        return lines;
    }

    private String mergeTextList(String original, List<String> additions) {
        Set<String> values = new LinkedHashSet<>();
        if (StringUtils.hasText(original)) {
            for (String part : original.split("[,，、/;；\\n]")) {
                String value = cleanValue(part);
                if (StringUtils.hasText(value)) {
                    values.add(value);
                }
            }
        }
        values.addAll(additions);
        return String.join(", ", normalizeTechStack(values));
    }

    private List<String> normalizeTechStack(Set<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String canonicalValue = canonicalizeTechItem(value);
            if (StringUtils.hasText(canonicalValue)) {
                normalized.add(canonicalValue);
            }
        }

        if (normalized.contains("Spring Boot")) {
            normalized.remove("Spring");
            normalized.remove("Boot");
        }
        if (normalized.contains("Spring Cloud")) {
            normalized.remove("Spring");
        }
        if (normalized.contains("Prompt Engineering")) {
            normalized.remove("Prompt");
            normalized.remove("Engineering");
        }
        if (normalized.contains("AI Agent")) {
            normalized.remove("AI");
            normalized.remove("Agent");
        }
        if (normalized.contains("MyBatis-Plus")) {
            normalized.remove("MyBatis");
            normalized.remove("Plus");
        }
        if (normalized.contains("Vue3")) {
            normalized.remove("Vue");
        }
        List<String> priority = List.of(
                "Java",
                "Spring Boot",
                "Vue3",
                "MySQL",
                "MyBatis-Plus",
                "AI Agent",
                "MCP",
                "LLM",
                "大模型 API",
                "Prompt Engineering"
        );
        List<String> result = new ArrayList<>();
        for (String item : priority) {
            if (normalized.remove(item)) {
                result.add(item);
            }
        }
        result.addAll(normalized);
        return result;
    }

    private String canonicalizeTechItem(String value) {
        String text = cleanValue(value);
        if (!StringUtils.hasText(text)) {
            return "";
        }

        String lower = text.toLowerCase();
        String compact = lower.replaceAll("[\\s_\\-]+", "");
        if ("springboot".equals(compact)) {
            return "Spring Boot";
        }
        if ("springcloud".equals(compact)) {
            return "Spring Cloud";
        }
        if ("promptengineering".equals(compact)) {
            return "Prompt Engineering";
        }
        if ("aiagent".equals(compact)) {
            return "AI Agent";
        }
        if ("mybatisplus".equals(compact)) {
            return "MyBatis-Plus";
        }
        if ("vue3".equals(compact)) {
            return "Vue3";
        }
        if ("mysql".equals(compact)) {
            return "MySQL";
        }
        if ("mcp".equals(compact)) {
            return "MCP";
        }
        if ("llm".equals(compact)) {
            return "LLM";
        }
        if ("大模型api".equals(compact) || "llmapi".equals(compact)) {
            return "大模型 API";
        }
        if ("java".equals(compact)) {
            return "Java";
        }
        if ("mybatis".equals(compact)) {
            return "MyBatis";
        }
        if ("spring".equals(compact)) {
            return "Spring";
        }
        if ("boot".equals(compact)) {
            return "Boot";
        }
        if ("prompt".equals(compact)) {
            return "Prompt";
        }
        if ("engineering".equals(compact)) {
            return "Engineering";
        }
        if ("ai".equals(compact)) {
            return "AI";
        }
        if ("agent".equals(compact)) {
            return "Agent";
        }
        if ("vue".equals(compact)) {
            return "Vue";
        }
        if ("plus".equals(compact)) {
            return "Plus";
        }
        return text;
    }

    private String limitAndJoin(List<String> values, int limit) {
        if (values.isEmpty()) {
            return "";
        }
        return String.join("；", values.stream().limit(limit).toList());
    }

    private String join(List<String> values) {
        return String.join(", ", values);
    }

    private String cleanValue(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceFirst("^[\\-_*#\\s]+", "")
                .replaceFirst("^[：:]+", "")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String text(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(item -> values.add(item.asText()));
            return String.join("；", values);
        }
        return node.asText();
    }
}
