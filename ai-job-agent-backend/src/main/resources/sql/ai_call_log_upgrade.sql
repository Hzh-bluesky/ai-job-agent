USE ai_job_agent;

ALTER TABLE ai_call_log
    ADD COLUMN attempt_count INT DEFAULT NULL COMMENT 'current attempt number' AFTER latency_ms,
    ADD COLUMN quality_score INT DEFAULT NULL COMMENT 'local evaluation score 0-100' AFTER attempt_count,
    ADD COLUMN evaluation_passed TINYINT DEFAULT NULL COMMENT '0 failed, 1 passed' AFTER quality_score,
    ADD COLUMN evaluation_issues TEXT COMMENT 'local evaluation issues' AFTER evaluation_passed,
    ADD COLUMN failure_type VARCHAR(50) DEFAULT NULL COMMENT 'failure type for retry/fallback decision' AFTER evaluation_issues,
    ADD COLUMN fallback_used TINYINT NOT NULL DEFAULT 0 COMMENT '0 no fallback, 1 fallback mock used' AFTER failure_type;

CREATE INDEX idx_eval_passed ON ai_call_log (evaluation_passed);
CREATE INDEX idx_failure_type ON ai_call_log (failure_type);
