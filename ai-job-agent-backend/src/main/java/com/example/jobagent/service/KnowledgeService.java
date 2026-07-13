package com.example.jobagent.service;

import com.example.jobagent.entity.KnowledgeChunk;

import java.util.List;

public interface KnowledgeService {

    void indexKnowledge(Long userId, String sourceType, Long sourceId, String title, String content);

    List<KnowledgeChunk> searchRelevantKnowledge(Long userId, String query, Integer limit);
}
