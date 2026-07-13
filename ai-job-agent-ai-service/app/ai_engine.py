import json
import os
import re
import time
from collections import OrderedDict
from typing import Any, Dict, List, Tuple

import httpx

from app.schemas import ChatRequest, ChatResponse, EvaluateResponse


DEFAULT_QWEN_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"

STOP_WORDS = {
    "岗位", "职责", "要求", "任职", "公司", "我们", "负责", "参与", "熟悉", "了解", "优先",
    "经验", "相关", "能力", "可以", "需要", "以及", "进行", "使用", "开发", "项目", "实习",
    "the", "and", "for", "with", "from", "this", "that", "you", "are", "your",
}

KNOWN_TERMS = [
    "Spring Boot", "MyBatis-Plus", "Prompt Engineering", "AI Agent", "大模型 API",
    "Java", "Vue3", "MySQL", "Redis", "JWT", "LLM", "MCP", "RAG", "FastAPI",
    "Spring Cloud", "Docker", "Python", "Agent", "Skill", "OpenAI",
]


async def chat(request: ChatRequest) -> ChatResponse:
    start = time.perf_counter()
    provider = (request.provider or "qwen").lower()
    if provider not in {"qwen", "dashscope"}:
        return ChatResponse(
            content=None,
            success=False,
            latencyMs=elapsed_ms(start),
            provider=request.provider,
            model=request.model,
            errorMessage=f"Unsupported provider: {request.provider}",
        )

    api_key = os.getenv("DASHSCOPE_API_KEY", "").strip()
    if not api_key:
        return ChatResponse(
            content=None,
            success=False,
            latencyMs=elapsed_ms(start),
            provider="QWEN",
            model=request.model,
            errorMessage="DASHSCOPE_API_KEY is not configured",
        )

    base_url = os.getenv("DASHSCOPE_BASE_URL", DEFAULT_QWEN_BASE_URL).rstrip("/")
    timeout_seconds = int(os.getenv("AI_SERVICE_TIMEOUT_SECONDS", "90"))
    prompt = build_prompt(request)
    payload = {
        "model": request.model or "qwen-plus",
        "temperature": 0.2,
        "max_tokens": 4096,
        "response_format": {"type": "json_object"},
        "messages": [
            {
                "role": "system",
                "content": (
                    f"You are {request.skillName} in AI Job Agent. "
                    "Return valid JSON only. Do not output Markdown fences or explanations. "
                    "Do not fabricate user experience."
                ),
            },
            {"role": "user", "content": prompt},
        ],
    }

    try:
        async with httpx.AsyncClient(timeout=timeout_seconds) as client:
            response = await client.post(
                f"{base_url}/chat/completions",
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json=payload,
            )
        if response.status_code >= 400:
            return ChatResponse(
                content=None,
                success=False,
                latencyMs=elapsed_ms(start),
                provider="QWEN",
                model=request.model,
                errorMessage=f"Qwen HTTP {response.status_code}: {response.text[:500]}",
            )
        data = response.json()
        content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
        return ChatResponse(
            content=normalize_json_content(content),
            success=bool(content),
            latencyMs=elapsed_ms(start),
            provider="QWEN",
            model=request.model,
            errorMessage=None if content else "Qwen returned empty content",
        )
    except Exception as exc:
        return ChatResponse(
            content=None,
            success=False,
            latencyMs=elapsed_ms(start),
            provider="QWEN",
            model=request.model,
            errorMessage=str(exc),
        )


def evaluate(skill_name: str, content: str) -> EvaluateResponse:
    issues: List[str] = []
    score = 100
    try:
        root = json.loads(content)
    except Exception as exc:
        return EvaluateResponse(score=0, passed=False, issues=[f"JSON parse failed: {exc}"])

    if not isinstance(root, dict):
        return EvaluateResponse(score=0, passed=False, issues=["JSON root must be an object"])

    skill = skill_name or ""
    if skill == "JDParseSkill":
        score -= require_text(root, "jobName", issues)
        score -= require_text(root, "techStack", issues)
        score -= require_text(root, "requirements", issues)
        score -= soft_text(root, "responsibilities", issues)
    elif skill == "ResumeMatchSkill":
        for field in ["overallScore", "techScore", "projectScore", "educationScore"]:
            score -= require_score(root, field, issues)
        for field in ["advantageAnalysis", "weaknessAnalysis", "suggestion"]:
            score -= require_text(root, field, issues)
        if "isRecommended" not in root:
            issues.append("missing field: isRecommended")
            score -= 15
    elif skill == "ResumeRewriteSkill":
        for field in ["rewrittenProject", "rewriteReason", "resumeVersion"]:
            score -= require_text(root, field, issues)
        if len(str(root.get("rewrittenProject", ""))) < 30:
            issues.append("rewrittenProject is too short")
            score -= 15
    elif skill == "GreetingGenerateSkill":
        score -= require_text(root, "greetingText", issues)
        length = len(str(root.get("greetingText", "")))
        if length and (length < 40 or length > 220):
            issues.append("greetingText length is not ideal")
            score -= 20
    elif skill == "InterviewQuestionSkill":
        score -= require_question_array(root, "technicalQuestions", 10, issues)
        score -= require_question_array(root, "projectQuestions", 5, issues)
        score -= require_question_array(root, "hrQuestions", 5, issues)

    serialized = json.dumps(root, ensure_ascii=False).lower()
    for placeholder in ["todo", "待补充", "待填写", "xxx", "n/a", "占位"]:
        if placeholder in serialized:
            issues.append(f"placeholder text found: {placeholder}")
            score -= 20
            break

    normalized_score = max(0, min(100, score))
    return EvaluateResponse(score=normalized_score, passed=normalized_score >= 70 and not has_missing_issue(issues), issues=issues)


def extract_keywords(text: str, limit: int) -> List[str]:
    if not text:
        return []
    keywords: "OrderedDict[str, None]" = OrderedDict()
    lower_text = text.lower()

    for term in KNOWN_TERMS:
        if term.lower() in lower_text:
            keywords[term] = None

    for token in re.split(r"[^\w+#.-]+", text):
        value = token.strip()
        if len(value) < 2:
            continue
        if value.lower() in STOP_WORDS:
            continue
        keywords[value] = None
        if len(keywords) >= limit:
            break

    return list(keywords.keys())[:limit]


def build_prompt(request: ChatRequest) -> str:
    if request.ragContext:
        return (
            f"{request.prompt}\n\n"
            "[RAG Context]\n"
            f"{request.ragContext}\n\n"
            "Use RAG context only as user-owned reference. Do not fabricate experience."
        )
    return request.prompt


def normalize_json_content(content: str) -> str:
    text = (content or "").strip()
    if text.startswith("```"):
        text = re.sub(r"^```[a-zA-Z]*\s*", "", text)
        text = re.sub(r"\s*```$", "", text).strip()
    object_start = text.find("{")
    array_start = text.find("[")
    starts = [index for index in [object_start, array_start] if index >= 0]
    if not starts:
        return text
    start = min(starts)
    end = max(text.rfind("}"), text.rfind("]"))
    if end > start:
        return text[start:end + 1].strip()
    return text


def require_text(root: Dict[str, Any], field: str, issues: List[str]) -> int:
    if not str(root.get(field, "")).strip():
        issues.append(f"missing field: {field}")
        return 25
    return 0


def soft_text(root: Dict[str, Any], field: str, issues: List[str]) -> int:
    if not str(root.get(field, "")).strip():
        issues.append(f"empty field: {field}")
        return 10
    return 0


def require_score(root: Dict[str, Any], field: str, issues: List[str]) -> int:
    value = root.get(field)
    if not isinstance(value, int):
        issues.append(f"missing or invalid score: {field}")
        return 20
    if value < 0 or value > 100:
        issues.append(f"score out of range: {field}")
        return 20
    return 0


def require_question_array(root: Dict[str, Any], field: str, expected: int, issues: List[str]) -> int:
    value = root.get(field)
    if not isinstance(value, list):
        issues.append(f"missing question array: {field}")
        return 25
    penalty = 0
    if len(value) < expected:
        issues.append(f"{field} count is {len(value)}, expected {expected}")
        penalty += min(25, (expected - len(value)) * 4)
    for index, item in enumerate(value, start=1):
        if not isinstance(item, dict) or not item.get("question") or not item.get("answerIdea"):
            issues.append(f"{field}[{index}] missing question or answerIdea")
            penalty += 5
    return penalty


def has_missing_issue(issues: List[str]) -> bool:
    return any(issue.startswith("missing") for issue in issues)


def elapsed_ms(start: float) -> int:
    return int((time.perf_counter() - start) * 1000)
