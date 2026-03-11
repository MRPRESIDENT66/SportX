package com.example.sportx.Service.Impl;

import com.example.sportx.Config.AiRagProperties;
import com.example.sportx.Entity.AiKnowledgeChunk;
import com.example.sportx.Entity.AiKnowledgeDocument;
import com.example.sportx.Service.AiChunkingService;
import com.example.sportx.Service.AiKnowledgeService;
import com.example.sportx.Service.AiRetrievalService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRetrievalServiceImpl implements AiRetrievalService {

    private static final int EMBEDDING_BATCH_SIZE = 10;

    private final AiRagProperties aiRagProperties;
    private final AiKnowledgeService aiKnowledgeService;
    private final AiChunkingService aiChunkingService;

    private final InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    @Override
    public boolean isConfigured() {
        return aiRagProperties.getEmbeddingApiKey() != null && !aiRagProperties.getEmbeddingApiKey().isBlank();
    }

    @Override
    public synchronized int rebuildIndex() {
        if (!isConfigured()) {
            return 0;
        }

        // 每次重建都从当前数据库和规则文档重新取材，保证索引内容和业务数据一致。
        List<AiKnowledgeDocument> documents = aiKnowledgeService.loadAllKnowledgeDocuments();
        List<AiKnowledgeChunk> chunks = aiChunkingService.chunkDocuments(documents);
        if (chunks.isEmpty()) {
            embeddingStore.removeAll();
            return 0;
        }

        List<TextSegment> segments = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (AiKnowledgeChunk chunk : chunks) {
            segments.add(toTextSegment(chunk));
            ids.add(chunk.getChunkId());
        }

        List<Embedding> embeddings = embedInBatches(segments);
        embeddingStore.removeAll();
        embeddingStore.addAll(ids, embeddings, segments);
        return chunks.size();
    }

    @Override
    public synchronized List<AiKnowledgeChunk> searchRelevantChunks(String question) {
        if (!isConfigured()) {
            throw new IllegalStateException("AI embedding model is not configured");
        }
        if (question == null || question.isBlank()) {
            return List.of();
        }
        // 当前先用内存向量库，所以首次查询时懒加载建索引即可。
        if (embeddingStore.isEmpty()) {
            rebuildIndex();
        }
        if (embeddingStore.isEmpty()) {
            return List.of();
        }

        Embedding queryEmbedding = embeddingModel().embed(question).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(aiRagProperties.getMaxResults())
                .minScore(aiRagProperties.getMinScore())
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<AiKnowledgeChunk> chunks = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : result.matches()) {
            if (match == null || match.embedded() == null) {
                continue;
            }
            chunks.add(fromTextSegment(match.embedded(), match.embeddingId()));
        }
        return reorderByQuestionIntent(question, chunks);
    }

    private OpenAiEmbeddingModel embeddingModel() {
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(aiRagProperties.getEmbeddingApiKey())
                .modelName(aiRagProperties.getEmbeddingModel());
        if (aiRagProperties.getEmbeddingBaseUrl() != null && !aiRagProperties.getEmbeddingBaseUrl().isBlank()) {
            builder.baseUrl(aiRagProperties.getEmbeddingBaseUrl());
        }
        return builder.build();
    }

    private List<Embedding> embedInBatches(List<TextSegment> segments) {
        List<Embedding> embeddings = new ArrayList<>();
        for (int start = 0; start < segments.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, segments.size());
            List<TextSegment> batch = segments.subList(start, end);
            Response<List<Embedding>> response = embeddingModel().embedAll(batch);
            embeddings.addAll(response.content());
        }
        return embeddings;
    }

    private List<AiKnowledgeChunk> reorderByQuestionIntent(String question, List<AiKnowledgeChunk> chunks) {
        if (chunks.isEmpty()) {
            return chunks;
        }
        String normalizedQuestion = question.toLowerCase(Locale.ROOT);
        boolean ruleQuestion = normalizedQuestion.contains("rule")
                || normalizedQuestion.contains("policy")
                || normalizedQuestion.contains("流程")
                || normalizedQuestion.contains("规则")
                || normalizedQuestion.contains("要求");
        if (!ruleQuestion) {
            return chunks;
        }

        List<AiKnowledgeChunk> ruleChunks = chunks.stream()
                .filter(chunk -> "rule".equalsIgnoreCase(chunk.getType()))
                .collect(Collectors.toList());
        List<AiKnowledgeChunk> otherChunks = chunks.stream()
                .filter(chunk -> !"rule".equalsIgnoreCase(chunk.getType()))
                .collect(Collectors.toList());

        if (ruleChunks.isEmpty()) {
            return chunks;
        }

        List<AiKnowledgeChunk> reordered = new ArrayList<>(chunks.size());
        reordered.addAll(ruleChunks);
        reordered.addAll(otherChunks);
        return reordered;
    }

    private TextSegment toTextSegment(AiKnowledgeChunk chunk) {
        Metadata metadata = new Metadata()
                .put("chunkId", defaultValue(chunk.getChunkId()))
                .put("documentId", defaultValue(chunk.getDocumentId()))
                .put("type", defaultValue(chunk.getType()))
                .put("title", defaultValue(chunk.getTitle()))
                .put("source", defaultValue(chunk.getSource()))
                .put("chunkIndex", chunk.getChunkIndex() == null ? -1 : chunk.getChunkIndex());
        return TextSegment.from(chunk.getContent(), metadata);
    }

    private AiKnowledgeChunk fromTextSegment(TextSegment segment, String fallbackChunkId) {
        Metadata metadata = segment.metadata();
        AiKnowledgeChunk chunk = new AiKnowledgeChunk();
        chunk.setChunkId(readString(metadata, "chunkId", fallbackChunkId));
        chunk.setDocumentId(readString(metadata, "documentId", ""));
        chunk.setType(readString(metadata, "type", ""));
        chunk.setTitle(readString(metadata, "title", ""));
        chunk.setContent(segment.text());
        chunk.setSource(readString(metadata, "source", ""));
        chunk.setChunkIndex(metadata.containsKey("chunkIndex") ? metadata.getInteger("chunkIndex") : null);
        return chunk;
    }

    private String readString(Metadata metadata, String key, String fallbackValue) {
        return metadata != null && metadata.containsKey(key) ? metadata.getString(key) : fallbackValue;
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
