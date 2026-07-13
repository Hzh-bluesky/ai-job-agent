package com.example.jobagent.agent.rag;

import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.entity.KnowledgeChunk;
import com.example.jobagent.entity.Resume;
import com.example.jobagent.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagContextBuilder {

    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_CHUNK_CONTENT_LENGTH = 800;

    private final KnowledgeService knowledgeService;

    public String build(Long userId, Resume resume, JobPost jobPost, JobAnalysis jobAnalysis) {
        String query = buildQuery(resume, jobPost, jobAnalysis);
        List<KnowledgeChunk> chunks = knowledgeService.searchRelevantKnowledge(userId, query, DEFAULT_LIMIT);
        if (chunks.isEmpty()) {
            log.info("[RagContextBuilder] no relevant knowledge userId={}, resumeId={}, jobPostId={}",
                    userId, resume == null ? null : resume.getId(), jobPost == null ? null : jobPost.getId());
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Relevant user career knowledge retrieved from the lightweight RAG store. Use as reference only and do not fabricate experience.\n");
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            builder.append("\n[Knowledge Chunk ").append(i + 1).append("]")
                    .append(" sourceType=").append(chunk.getSourceType())
                    .append(", sourceId=").append(chunk.getSourceId())
                    .append(", title=").append(defaultIfBlank(chunk.getTitle(), "Untitled"))
                    .append("\n")
                    .append(trimToMax(chunk.getContent(), MAX_CHUNK_CONTENT_LENGTH))
                    .append("\n");
        }
        String ragContext = builder.toString();
        log.info("[RagContextBuilder] built rag context userId={}, resumeId={}, jobPostId={}, chunkCount={}, length={}",
                userId,
                resume == null ? null : resume.getId(),
                jobPost == null ? null : jobPost.getId(),
                chunks.size(),
                ragContext.length());
        return ragContext;
    }

    private String buildQuery(Resume resume, JobPost jobPost, JobAnalysis jobAnalysis) {
        StringBuilder query = new StringBuilder();
        append(query, resume == null ? null : resume.getTitle());
        append(query, resume == null ? null : resume.getName());
        append(query, resume == null ? null : resume.getSchool());
        append(query, resume == null ? null : resume.getMajor());
        append(query, resume == null ? null : resume.getTechStack());
        append(query, resume == null ? null : resume.getProjectExperience());
        append(query, jobPost == null ? null : jobPost.getCompanyName());
        append(query, jobPost == null ? null : jobPost.getJobName());
        append(query, jobPost == null ? null : jobPost.getJdText());
        append(query, jobAnalysis == null ? null : jobAnalysis.getTechStack());
        append(query, jobAnalysis == null ? null : jobAnalysis.getRequirements());
        append(query, jobAnalysis == null ? null : jobAnalysis.getResponsibilities());
        append(query, jobAnalysis == null ? null : jobAnalysis.getBonusPoints());
        return query.toString();
    }

    private void append(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(value).append('\n');
        }
    }

    private String trimToMax(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
