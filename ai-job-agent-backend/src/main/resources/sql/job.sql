USE ai_job_agent;

CREATE TABLE IF NOT EXISTS job_post (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  jd_text LONGTEXT NOT NULL COMMENT '岗位JD原文',
  source VARCHAR(50) DEFAULT NULL COMMENT '来源平台，如Boss直聘、拉勾、牛客',
  source_link VARCHAR(500) DEFAULT NULL COMMENT '岗位来源链接',
  company_name VARCHAR(100) DEFAULT NULL COMMENT '公司名称',
  job_name VARCHAR(100) DEFAULT NULL COMMENT '岗位名称',
  city VARCHAR(50) DEFAULT NULL COMMENT '城市',
  salary VARCHAR(50) DEFAULT NULL COMMENT '薪资',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  KEY idx_user_id (user_id),
  KEY idx_user_source_link (user_id, source_link),
  KEY idx_user_update_time (user_id, update_time),
  KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位原文表';

CREATE TABLE IF NOT EXISTS job_analysis (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  job_post_id BIGINT NOT NULL COMMENT '岗位原文ID',
  company_name VARCHAR(100) DEFAULT NULL COMMENT '公司名称',
  job_name VARCHAR(100) DEFAULT NULL COMMENT '岗位名称',
  city VARCHAR(50) DEFAULT NULL COMMENT '城市',
  salary VARCHAR(50) DEFAULT NULL COMMENT '薪资',
  education VARCHAR(50) DEFAULT NULL COMMENT '学历要求',
  internship_cycle VARCHAR(100) DEFAULT NULL COMMENT '实习周期',
  tech_stack TEXT COMMENT '技术栈',
  responsibilities TEXT COMMENT '岗位职责',
  requirements TEXT COMMENT '任职要求',
  bonus_points TEXT COMMENT '加分项',
  risk_points TEXT COMMENT '风险点',
  raw_result LONGTEXT COMMENT 'AI原始返回JSON字符串',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  KEY idx_user_id (user_id),
  KEY idx_job_post_id (job_post_id),
  KEY idx_user_job_post (user_id, job_post_id),
  KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位分析结果表';
