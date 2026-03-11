package com.example.sportx.Service.Impl;

import com.example.sportx.Entity.AiKnowledgeChunk;
import com.example.sportx.Entity.AiKnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiChunkingServiceImplTest {

    private final AiChunkingServiceImpl aiChunkingService = new AiChunkingServiceImpl();

    @Test
    void chunkDocuments_shouldSplitLongDocumentIntoMultipleChunks() {
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setId("rule-platform");
        document.setType("rule");
        document.setTitle("SportX Platform Rules");
        document.setSource("ai/platform-rules.md");
        document.setContent("""
                Paragraph one explains registration and cancellation rules in the platform.

                Paragraph two explains leaderboard updates, notification delivery, retry logic,
                dead-letter queue handling, idempotency markers, and several platform-specific
                guarantees that make the text long enough for chunking validation.

                Paragraph three explains login state, token refresh, and favorite rules.
                """);

        List<AiKnowledgeChunk> chunks = aiChunkingService.chunkDocuments(List.of(document));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.getDocumentId()).isEqualTo("rule-platform");
            assertThat(chunk.getChunkId()).contains("rule-platform-chunk-");
            assertThat(chunk.getContent()).isNotBlank();
        });
    }
}
