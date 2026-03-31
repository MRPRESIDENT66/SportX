package com.example.sportx.Service;

import com.example.sportx.Entity.AiKnowledgeChunk;

import java.util.List;

public interface AiGenerationService {

    boolean isConfigured();

    String generateAnswer(String question, List<AiKnowledgeChunk> chunks);
}
