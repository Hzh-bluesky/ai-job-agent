# AI Job Agent AI Service

Independent Python FastAPI service for AI engineering capabilities used by the Java Spring Boot backend.

The Java backend remains the main business service. This service exposes:

- `GET /health`
- `POST /api/ai/chat`
- `POST /api/ai/evaluate`
- `POST /api/ai/keywords`

## Setup

Python 3.7+ is supported by the pinned dependencies in `requirements.txt`.

```bash
cd ai-job-agent-ai-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

Set Qwen API key:

```powershell
setx DASHSCOPE_API_KEY "your-api-key"
```

Open a new terminal after `setx`.

## Run

```bash
cd ai-job-agent-ai-service
.venv\Scripts\activate
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Swagger UI:

```text
http://localhost:8000/docs
```

## Test Examples

Health:

```bash
curl http://localhost:8000/health
```

Chat:

```bash
curl -X POST http://localhost:8000/api/ai/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"provider\":\"qwen\",\"model\":\"qwen-plus\",\"skillName\":\"GreetingGenerateSkill\",\"prompt\":\"Return JSON only: {\\\"greetingText\\\":\\\"hello\\\"}\"}"
```

Evaluate:

```bash
curl -X POST http://localhost:8000/api/ai/evaluate ^
  -H "Content-Type: application/json" ^
  -d "{\"skillName\":\"GreetingGenerateSkill\",\"content\":\"{\\\"greetingText\\\":\\\"Hello, I am familiar with Java and Spring Boot and hope to discuss this internship opportunity.\\\"}\"}"
```

Keywords:

```bash
curl -X POST http://localhost:8000/api/ai/keywords ^
  -H "Content-Type: application/json" ^
  -d "{\"text\":\"Java Spring Boot Vue3 MySQL AI Agent MCP LLM project experience\",\"limit\":10}"
```

## Java Backend Provider

In the Java backend:

```yaml
llm:
  provider: ${LLM_PROVIDER:mock}
  fastapi:
    base-url: http://localhost:8000
    provider: qwen
    model: qwen-plus
    timeout-seconds: 90
```

Windows:

```powershell
setx LLM_PROVIDER fastapi
```

Restart the Java backend after changing environment variables.
