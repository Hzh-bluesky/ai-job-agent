USE ai_job_agent;

CREATE TABLE IF NOT EXISTS match_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  resume_id BIGINT NOT NULL COMMENT '简历ID',
  job_post_id BIGINT NOT NULL COMMENT '岗位ID',
  job_analysis_id BIGINT NOT NULL COMMENT '岗位分析ID',
  overall_score INT NOT NULL DEFAULT 0 COMMENT '综合匹配度，0-100',
  tech_score INT NOT NULL DEFAULT 0 COMMENT '技术栈匹配度',
  project_score INT NOT NULL DEFAULT 0 COMMENT '项目经历匹配度',
  education_score INT NOT NULL DEFAULT 0 COMMENT '学历/年级匹配度',
  advantage_analysis TEXT COMMENT '优势分析',
  weakness_analysis TEXT COMMENT '不足分析',
  suggestion TEXT COMMENT '补强建议',
  is_recommended TINYINT NOT NULL DEFAULT 0 COMMENT '是否建议投递：0否 1是',
  raw_result LONGTEXT COMMENT 'AI原始返回JSON字符串',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  KEY idx_user_id (user_id),
  KEY idx_resume_id (resume_id),
  KEY idx_job_post_id (job_post_id),
  KEY idx_job_analysis_id (job_analysis_id),
  KEY idx_user_create_time (user_id, create_time),
  KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历岗位匹配报告表';
