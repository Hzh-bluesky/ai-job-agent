package com.example.jobagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.jobagent.entity.KnowledgeChunk;
import com.example.jobagent.mapper.KnowledgeChunkMapper;
import com.example.jobagent.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final int MAX_CONTENT_LENGTH = 12000;
    private static final int DEFAULT_LIMIT = 6;
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[^\\p{L}\\p{N}+#.-]+");

    private final KnowledgeChunkMapper knowledgeChunkMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void indexKnowledge(Long userId, String sourceType, Long sourceId, String title, String content) {
        if (userId == null || !StringUtils.hasText(sourceType) || sourceId == null || !StringUtils.hasText(content)) {
            return;
        }

        String normalizedContent = trimToMax(content.trim(), MAX_CONTENT_LENGTH);
        KnowledgeChunk existing = knowledgeChunkMapper.selectOne(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getUserId, userId)
                .eq(KnowledgeChunk::getSourceType, sourceType)
                .eq(KnowledgeChunk::getSourceId, sourceId)
                .last("LIMIT 1"));

        if (existing == null) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setUserId(userId);
            chunk.setSourceType(sourceType);
            chunk.setSourceId(sourceId);
            chunk.setTitle(defaultIfBlank(title, sourceType + "#" + sourceId));
            chunk.setContent(normalizedContent);
            chunk.setKeywords(extractKeywords(defaultIfBlank(title, "") + "\n" + normalizedContent));
            chunk.setScore(0);
            knowledgeChunkMapper.insert(chunk);
            log.info("[KnowledgeService] indexed knowledge userId={}, sourceType={}, sourceId={}, chunkId={}",
                    userId, sourceType, sourceId, chunk.getId());
            return;
        }

        existing.setTitle(defaultIfBlank(title, existing.getTitle()));
        existing.setContent(normalizedContent);
        existing.setKeywords(extractKeywords(defaultIfBlank(title, "") + "\n" + normalizedContent));
        knowledgeChunkMapper.updateById(existing);
        log.info("[KnowledgeService] updated knowledge userId={}, sourceType={}, sourceId={}, chunkId={}",
                userId, sourceType, sourceId, existing.getId());
    }

    @Override
    public List<KnowledgeChunk> searchRelevantKnowledge(Long userId, String query, Integer limit) {
        if (userId == null || !StringUtils.hasText(query)) {
            return List.of();
        }

        List<String> terms = splitTerms(query);
        int pageSize = Math.max(1, limit == null ? DEFAULT_LIMIT : limit);
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getUserId, userId)
                .orderByDesc(KnowledgeChunk::getUpdateTime)
                .last("LIMIT 80");

        if (!terms.isEmpty()) {
            wrapper.and(group -> {
                String firstTerm = terms.get(0);
                group.like(KnowledgeChunk::getTitle, firstTerm)
                        .or().like(KnowledgeChunk::getContent, firstTerm)
                        .or().like(KnowledgeChunk::getKeywords, firstTerm);

                for (int i = 1; i < terms.size(); i++) {
                    String term = terms.get(i);
                    group.or(item -> item.like(KnowledgeChunk::getTitle, term)
                            .or().like(KnowledgeChunk::getContent, term)
                            .or().like(KnowledgeChunk::getKeywords, term));
                }
            });
        }

        return knowledgeChunkMapper.selectList(wrapper).stream()
                .map(chunk -> {
                    chunk.setScore(calculateScore(chunk, terms));
                    return chunk;
                })
                .sorted(Comparator.comparing(KnowledgeChunk::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(KnowledgeChunk::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(pageSize)
                .toList();
    }

    private int calculateScore(KnowledgeChunk chunk, List<String> terms) {
        if (terms.isEmpty()) {
            return 1;
        }
        String text = (defaultIfBlank(chunk.getTitle(), "") + "\n"
                + defaultIfBlank(chunk.getContent(), "") + "\n"
                + defaultIfBlank(chunk.getKeywords(), "")).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            String normalized = term.toLowerCase(Locale.ROOT);
            if (text.contains(normalized)) {
                score += 10;
            }
        }
        return score;
    }

    private String extractKeywords(String text) {
        return String.join(",", splitTerms(text).stream().limit(30).toList());
    }

    private List<String> splitTerms(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        Arrays.stream(SPLIT_PATTERN.split(text))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(item -> item.length() >= 2)
                .limit(80)
                .forEach(terms::add);
        return terms.stream().toList();
    }

    private String trimToMax(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
