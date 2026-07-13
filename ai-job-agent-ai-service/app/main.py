from fastapi import FastAPI

from app.ai_engine import chat, evaluate, extract_keywords
from app.schemas import (
    ChatRequest,
    ChatResponse,
    EvaluateRequest,
    EvaluateResponse,
    KeywordsRequest,
    KeywordsResponse,
)

app = FastAPI(title="AI Job Agent AI Service", version="0.1.0")


@app.get("/health")
async def health():
    return {"status": "ok", "service": "ai-job-agent-ai-service"}


@app.post("/api/ai/chat", response_model=ChatResponse)
async def ai_chat(request: ChatRequest):
    return await chat(request)


@app.post("/api/ai/evaluate", response_model=EvaluateResponse)
async def ai_evaluate(request: EvaluateRequest):
    return evaluate(request.skillName, request.content)


@app.post("/api/ai/keywords", response_model=KeywordsResponse)
async def ai_keywords(request: KeywordsRequest):
    return KeywordsResponse(keywords=extract_keywords(request.text, request.limit))
