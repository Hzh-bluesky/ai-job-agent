package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("knowledge_chunk")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeChunk extends BaseEntity {

    private Long userId;

    private String sourceType;

    private Long sourceId;

    private String title;

    private String content;

    private String keywords;

    private Integer score;
}
