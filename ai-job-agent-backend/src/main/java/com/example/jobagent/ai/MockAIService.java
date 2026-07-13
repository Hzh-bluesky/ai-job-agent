package com.example.jobagent.ai;

import org.springframework.stereotype.Service;

@Service("mockAIService")
public class MockAIService implements LLMService {

    @Override
    public LLMResponse chat(LLMRequest request) {
        String skillName = request.getSkillName();
        String content;
        if ("ResumeMatchSkill".equals(skillName)) {
            content = buildMockResumeMatchJson();
        } else if ("ResumeRewriteSkill".equals(skillName)) {
            content = buildMockResumeRewriteJson();
        } else if ("GreetingGenerateSkill".equals(skillName)) {
            content = buildMockGreetingJson();
        } else if ("InterviewQuestionSkill".equals(skillName)) {
            content = buildMockInterviewQuestionJson();
        } else {
            content = buildMockJDAnalysisJson();
        }

        return LLMResponse.builder()
                .provider("MOCK")
                .model("mock-ai-service-v1")
                .success(true)
                .statusCode(200)
                .content(content)
                .rawResponseBody(content)
                .build();
    }

    private String buildMockJDAnalysisJson() {
        return """
                {
                  "companyName": "示例科技有限公司",
                  "jobName": "AI Agent应用开发实习生",
                  "city": "杭州",
                  "salary": "150-200/天",
                  "education": "本科及以上",
                  "internshipCycle": "每周4天，实习3个月以上",
                  "techStack": "Java, Spring Boot, Vue3, MySQL, LLM, Prompt Engineering",
                  "responsibilities": "参与AI Agent应用后端接口开发，负责岗位分析、简历匹配等业务模块落地。",
                  "requirements": "熟悉Java基础、Spring Boot、MySQL，了解Vue3和大模型应用开发流程，有完整项目经验优先。",
                  "bonusPoints": "有AI Agent、RAG、Prompt优化、开源项目或实习项目经验会加分。",
                  "riskPoints": "岗位强调完整项目交付能力，如果只有课程作业经验，需要重点补强项目描述。"
                }
                """;
    }

    private String buildMockResumeMatchJson() {
        return """
                {
                  "overallScore": 78,
                  "techScore": 80,
                  "projectScore": 75,
                  "educationScore": 85,
                  "advantageAnalysis": "用户具备Java、Spring Boot、MySQL、Vue3项目经验，与岗位基础技术栈匹配。",
                  "weaknessAnalysis": "用户缺少真实大模型API、MCP、企业级Agent工作流经验。",
                  "suggestion": "建议补充一个AI Agent项目Demo，并在简历中突出Agent、Skill、LLM调用链设计。",
                  "isRecommended": 1
                }
                """;
    }

    private String buildMockResumeRewriteJson() {
        return """
                {
                  "originalProject": "AI求职投递助手项目，负责后端登录鉴权、JWT认证和简历模块开发。",
                  "rewrittenProject": "基于Spring Boot + MyBatis-Plus + MySQL设计并实现AI求职投递助手Agent后端模块，负责JWT登录鉴权、用户数据隔离、简历CRUD、岗位分析及匹配报告等核心接口开发，并通过Agent + Skill分层结构封装JD解析与简历匹配能力。",
                  "rewriteReason": "优化后突出技术栈、业务场景、职责边界和Agent架构能力，更符合AI Agent应用开发和Java后端实习岗位要求。",
                  "resumeVersion": "AI求职投递助手Agent：基于Spring Boot、MyBatis-Plus、MySQL、JWT实现求职场景下的后端服务，支持用户登录鉴权、简历管理、岗位JD分析、简历匹配报告生成等功能；采用JobAgent + Skill架构封装AI能力，提升系统扩展性。"
                }
                """;
    }

    private String buildMockGreetingJson() {
        return """
                {
                  "greetingText": "您好，我是软件工程专业学生，熟悉Java、Spring Boot、MySQL、Vue3，近期做了一个AI求职投递助手Agent项目，包含JWT鉴权、简历管理、岗位JD分析、简历匹配和项目经历优化等功能。看到贵公司的AI Agent应用开发实习岗位，感觉和我的项目方向比较匹配，希望有机会进一步沟通。"
                }
                """;
    }

    private String buildMockInterviewQuestionJson() {
        return """
                {
                  "technicalQuestions": [
                    {
                      "question": "你项目中的JWT鉴权流程是怎么设计的？",
                      "answerIdea": "可以从登录生成token、前端携带Authorization、后端拦截器解析token、UserContext保存用户信息、接口按userId做数据隔离几个方面回答。"
                    },
                    {
                      "question": "MyBatis-Plus在你的项目里主要解决了什么问题？",
                      "answerIdea": "可以说明BaseMapper减少CRUD样板代码，LambdaQueryWrapper提升查询字段安全性，分页插件支持列表查询，TableLogic实现逻辑删除。"
                    },
                    {
                      "question": "你是怎么做用户数据隔离的？",
                      "answerIdea": "可以强调所有业务表都保存user_id，请求从JWT解析当前用户，查询、详情、删除和更新都带userId条件，避免越权访问。"
                    },
                    {
                      "question": "JobAgent和Skill的职责边界是什么？",
                      "answerIdea": "可以说明JobAgent负责流程编排，Skill负责单一AI能力，LLMService负责统一模型调用，这样后续扩展面试题、话术、简历优化更清晰。"
                    },
                    {
                      "question": "MockAIService后期如何替换成真实大模型API？",
                      "answerIdea": "可以说明只需要新增DeepSeek或OpenAI等LLMService实现，保持LLMRequest和LLMResponse结构稳定，业务层不用感知模型供应商。"
                    },
                    {
                      "question": "Spring Boot拦截器在JWT鉴权里起什么作用？",
                      "answerIdea": "可以从拦截受保护接口、读取Authorization、校验token、写入UserContext、请求结束清理ThreadLocal几个步骤回答。"
                    },
                    {
                      "question": "你为什么把AI返回结果保存rawResult？",
                      "answerIdea": "可以说明rawResult便于问题排查、结果复盘、后期重新解析字段，也方便比较不同模型或Prompt版本的输出差异。"
                    },
                    {
                      "question": "如果岗位JD解析失败，你会怎么处理？",
                      "answerIdea": "可以从LLM返回为空、JSON解析异常、业务异常提示、事务回滚、记录日志和后续接入ai_call_log几个方面回答。"
                    },
                    {
                      "question": "Vue3前端调用这些接口时，token应该如何管理？",
                      "answerIdea": "可以说明登录后保存token，Axios请求拦截器统一加Authorization，响应401时清理登录状态并跳回登录页。"
                    },
                    {
                      "question": "MySQL表设计里为什么每张业务表都要有deleted字段？",
                      "answerIdea": "可以说明逻辑删除能保留历史数据，避免误删，同时MyBatis-Plus的@TableLogic可以让普通查询自动过滤已删除记录。"
                    }
                  ],
                  "projectQuestions": [
                    {
                      "question": "你的AI求职投递助手Agent项目为什么要拆成JobAgent和Skill？",
                      "answerIdea": "可以说明Agent负责流程编排，Skill负责具体能力，后期方便扩展JD解析、简历匹配、打招呼话术、面试题生成等能力。"
                    },
                    {
                      "question": "你在简历匹配模块里是如何组织调用链的？",
                      "answerIdea": "可以按MatchController到MatchService，再到JobAgent、ResumeMatchSkill、LLMService、MockAIService的顺序说明。"
                    },
                    {
                      "question": "如果用户传入别人的resumeId或jobPostId，你的项目怎么防止越权？",
                      "answerIdea": "可以说明Service会先用id和当前userId同时查询资源，查不到就返回无权访问，后续保存记录也必须写入当前userId。"
                    },
                    {
                      "question": "简历优化模块如何保证不夸大经历？",
                      "answerIdea": "可以说明Prompt里明确限制只能基于已有项目内容优化表达，Service保存原始项目描述，前端可做原文和优化结果对照。"
                    },
                    {
                      "question": "这个项目如果继续扩展真实AI调用，你会怎么设计配置？",
                      "answerIdea": "可以说明通过配置选择provider和model，新增具体LLMService实现，并记录请求、响应、耗时和token消耗到ai_call_log。"
                    }
                  ],
                  "hrQuestions": [
                    {
                      "question": "你为什么想投AI Agent应用开发实习？",
                      "answerIdea": "可以结合自己做过AI求职投递助手Agent项目，以及对Vibe Coding、AI编程工具和业务自动化的兴趣来回答。"
                    },
                    {
                      "question": "你觉得自己和这个岗位最匹配的地方是什么？",
                      "answerIdea": "可以从Java后端基础、Spring Boot项目经验、Vue3了解程度、AI Agent分层设计实践几个方面回答。"
                    },
                    {
                      "question": "如果入职后遇到不熟悉的大模型API，你会怎么学习？",
                      "answerIdea": "可以说明先读官方文档和示例，做最小Demo，再封装统一接口，最后接入业务并补充异常处理和日志。"
                    },
                    {
                      "question": "你在项目中遇到过什么比较难的问题？",
                      "answerIdea": "可以选择JWT鉴权、用户数据隔离、Agent+Skill分层或AI JSON解析稳定性，讲清楚问题、方案和结果。"
                    },
                    {
                      "question": "你未来希望在实习中提升什么能力？",
                      "answerIdea": "可以回答希望提升真实业务需求拆解、工程化代码质量、AI应用落地、团队协作和线上问题排查能力。"
                    }
                  ]
                }
                """;
    }
}
