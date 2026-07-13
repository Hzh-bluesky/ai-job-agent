USE ai_job_agent;

ALTER TABLE job_post
    ADD COLUMN source_link VARCHAR(500) DEFAULT NULL COMMENT '岗位来源链接' AFTER source;

ALTER TABLE job_post
    ADD INDEX idx_user_source_link (user_id, source_link);
