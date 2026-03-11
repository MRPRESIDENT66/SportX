package com.example.sportx.Service;

import com.example.sportx.Entity.AiKnowledgeChunk;

import java.util.List;

public interface AiRetrievalService {

    boolean isConfigured();

    int rebuildIndex();

    List<AiKnowledgeChunk> searchRelevantChunks(String question);
}
