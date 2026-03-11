package com.example.sportx.Service.Impl;

import com.example.sportx.Entity.AiKnowledgeChunk;
import com.example.sportx.Entity.AiKnowledgeDocument;
import com.example.sportx.Service.AiChunkingService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiChunkingServiceImpl implements AiChunkingService {

    private static final int MAX_CHUNK_SIZE = 500;
    private static final int MAX_OVERLAP_SIZE = 80;

    private final DocumentByParagraphSplitter documentSplitter =
            new DocumentByParagraphSplitter(MAX_CHUNK_SIZE, MAX_OVERLAP_SIZE);

    @Override
    public List<AiKnowledgeChunk> chunkDocuments(List<AiKnowledgeDocument> documents) {
        List<AiKnowledgeChunk> chunks = new ArrayList<>();
        if (documents == null || documents.isEmpty()) {
            return chunks;
        }

        for (AiKnowledgeDocument document : documents) {
            if (document == null || isBlank(document.getContent())) {
                continue;
            }
            chunks.addAll(splitSingleDocument(document));
        }
        return chunks;
    }

    private List<AiKnowledgeChunk> splitSingleDocument(AiKnowledgeDocument knowledgeDocument) {
        List<AiKnowledgeChunk> result = new ArrayList<>();
        Document document = Document.from(
                knowledgeDocument.getContent(),
                Metadata.from(Map.of(
                        "documentId", defaultValue(knowledgeDocument.getId()),
                        "type", defaultValue(knowledgeDocument.getType()),
                        "title", defaultValue(knowledgeDocument.getTitle()),
                        "source", defaultValue(knowledgeDocument.getSource())
                ))
        );

        List<TextSegment> segments = documentSplitter.split(document);
        for (int i = 0; i < segments.size(); i++) {
            result.add(buildChunk(knowledgeDocument, segments.get(i), i));
        }
        return result;
    }

    private AiKnowledgeChunk buildChunk(AiKnowledgeDocument document, TextSegment segment, int chunkIndex) {
        AiKnowledgeChunk chunk = new AiKnowledgeChunk();
        chunk.setChunkId(document.getId() + "-chunk-" + chunkIndex);
        chunk.setDocumentId(document.getId());
        chunk.setType(document.getType());
        chunk.setTitle(document.getTitle());
        chunk.setContent(segment.text());
        chunk.setSource(document.getSource());
        chunk.setChunkIndex(chunkIndex);
        return chunk;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
