USE ai_job_agent;

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'primary key',
    user_id BIGINT NOT NULL COMMENT 'user id',
    source_type VARCHAR(50) NOT NULL COMMENT 'source type',
    source_id BIGINT NOT NULL COMMENT 'source id',
    title VARCHAR(255) DEFAULT NULL COMMENT 'chunk title',
    content LONGTEXT NOT NULL COMMENT 'chunk content',
    keywords VARCHAR(1000) DEFAULT NULL COMMENT 'simple keywords',
    score INT DEFAULT 0 COMMENT 'last retrieval score',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'logic delete',
    KEY idx_user_source (user_id, source_type, source_id),
    KEY idx_user_update_time (user_id, update_time),
    KEY idx_deleted (deleted),
    FULLTEXT KEY ft_title_content_keywords (title, content, keywords)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='lightweight rag knowledge chunk';
