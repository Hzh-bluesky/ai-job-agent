USE ai_job_agent;

CREATE TABLE IF NOT EXISTS apply_plan_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'primary key',
    user_id BIGINT NOT NULL COMMENT 'user id',
    resume_id BIGINT NOT NULL COMMENT 'resume id',
    job_post_id BIGINT NOT NULL COMMENT 'job post id',
    match_report_id BIGINT DEFAULT NULL COMMENT 'match report id',
    resume_rewrite_record_id BIGINT DEFAULT NULL COMMENT 'resume rewrite record id',
    greeting_record_id BIGINT DEFAULT NULL COMMENT 'greeting record id',
    interview_question_record_id BIGINT DEFAULT NULL COMMENT 'interview question record id',
    application_record_id BIGINT DEFAULT NULL COMMENT 'application record id',
    status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS' COMMENT 'plan status',
    next_step_suggestion TEXT COMMENT 'next step suggestion',
    error_message TEXT COMMENT 'error message',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'logic delete',
    KEY idx_user_id (user_id),
    KEY idx_resume_id (resume_id),
    KEY idx_job_post_id (job_post_id),
    KEY idx_user_create_time (user_id, create_time),
    KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='one click apply plan record';
