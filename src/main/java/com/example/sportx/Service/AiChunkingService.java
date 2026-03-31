package com.example.sportx.Service;

import com.example.sportx.Entity.AiKnowledgeChunk;
import com.example.sportx.Entity.AiKnowledgeDocument;

import java.util.List;

public interface AiChunkingService {

    List<AiKnowledgeChunk> chunkDocuments(List<AiKnowledgeDocument> documents);
}
