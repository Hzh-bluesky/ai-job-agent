USE ai_job_agent;

CREATE TABLE IF NOT EXISTS interview_question_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  job_post_id BIGINT NOT NULL COMMENT '岗位ID',
  technical_questions LONGTEXT COMMENT '技术面试题JSON字符串',
  project_questions LONGTEXT COMMENT '项目追问题JSON字符串',
  hr_questions LONGTEXT COMMENT 'HR常见问题JSON字符串',
  raw_result LONGTEXT COMMENT 'AI原始返回JSON字符串',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  KEY idx_user_id (user_id),
  KEY idx_job_post_id (job_post_id),
  KEY idx_user_create_time (user_id, create_time),
  KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题生成记录表';
