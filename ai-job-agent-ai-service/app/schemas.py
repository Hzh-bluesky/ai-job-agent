from typing import List, Optional

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    provider: str = Field(default="qwen")
    model: str = Field(default="qwen-plus")
    skillName: str
    prompt: str
    ragContext: Optional[str] = None


class ChatResponse(BaseModel):
    content: Optional[str] = None
    success: bool
    latencyMs: int
    provider: str
    model: str
    errorMessage: Optional[str] = None


class EvaluateRequest(BaseModel):
    skillName: str
    content: str


class EvaluateResponse(BaseModel):
    score: int
    passed: bool
    issues: List[str] = Field(default_factory=list)


class KeywordsRequest(BaseModel):
    text: str
    limit: int = Field(default=20, ge=1, le=80)


class KeywordsResponse(BaseModel):
    keywords: List[str]
