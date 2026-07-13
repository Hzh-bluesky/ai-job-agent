USE ai_job_agent;

CREATE TABLE IF NOT EXISTS greeting_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  resume_id BIGINT NOT NULL COMMENT '简历ID',
  job_post_id BIGINT NOT NULL COMMENT '岗位ID',
  greeting_text VARCHAR(500) NOT NULL COMMENT 'Boss直聘打招呼话术',
  raw_result LONGTEXT COMMENT 'AI原始返回JSON字符串',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  KEY idx_user_id (user_id),
  KEY idx_resume_id (resume_id),
  KEY idx_job_post_id (job_post_id),
  KEY idx_user_create_time (user_id, create_time),
  KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Boss直聘打招呼话术记录表';
