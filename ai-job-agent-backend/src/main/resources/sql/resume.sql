USE ai_job_agent;

CREATE TABLE IF NOT EXISTS resume (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  title VARCHAR(100) NOT NULL COMMENT '简历名称',
  name VARCHAR(50) DEFAULT NULL COMMENT '姓名',
  school VARCHAR(100) DEFAULT NULL COMMENT '学校',
  major VARCHAR(100) DEFAULT NULL COMMENT '专业',
  grade VARCHAR(50) DEFAULT NULL COMMENT '年级',
  tech_stack TEXT COMMENT '技术栈',
  project_experience TEXT COMMENT '项目经历',
  internship_experience TEXT COMMENT '实习经历',
  self_introduction TEXT COMMENT '自我介绍',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认简历：0否 1是',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  KEY idx_user_id (user_id),
  KEY idx_user_default (user_id, is_default),
  KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历表';
