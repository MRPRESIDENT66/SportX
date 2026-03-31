package com.example.sportx.Service.Impl;

import com.example.sportx.Entity.dto.AiAskRequestDto;
import com.example.sportx.Entity.vo.AiAskResponseVo;
import com.example.sportx.Entity.vo.AiSourceVo;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.AiKnowledgeChunk;
import com.example.sportx.Service.AiGenerationService;
import com.example.sportx.Service.AiService;
import com.example.sportx.Service.AiRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiRetrievalService aiRetrievalService;
    private final AiGenerationService aiGenerationService;

    @Override
    public Result<AiAskResponseVo> ask(AiAskRequestDto requestDto) {
        if (!aiRetrievalService.isConfigured()) {
            return Result.error("AI embedding is not configured. Please set ai.rag.embedding-api-key first");
        }
        if (!aiGenerationService.isConfigured()) {
            return Result.error("AI chat model is not configured. Please set ai.rag.chat-api-key and ai.rag.chat-model first");
        }

        List<AiKnowledgeChunk> chunks = aiRetrievalService.searchRelevantChunks(requestDto.getQuestion());
        if (chunks.isEmpty()) {
            return Result.error("No relevant knowledge was found for the question");
        }

        AiAskResponseVo responseVo = new AiAskResponseVo();
        responseVo.setAnswer(aiGenerationService.generateAnswer(requestDto.getQuestion(), chunks));
        responseVo.setSources(toSources(chunks));
        return Result.success(responseVo);
    }

    private List<AiSourceVo> toSources(List<AiKnowledgeChunk> chunks) {
        Map<String, AiSourceVo> sources = new LinkedHashMap<>();
        for (AiKnowledgeChunk chunk : chunks) {
            String sourceKey = chunk.getDocumentId() == null ? chunk.getChunkId() : chunk.getDocumentId();
            if (sources.containsKey(sourceKey)) {
                continue;
            }
            AiSourceVo source = new AiSourceVo();
            source.setType(chunk.getType());
            source.setId(chunk.getDocumentId());
            source.setTitle(chunk.getTitle());
            source.setSnippet(chunk.getContent());
            sources.put(sourceKey, source);
        }
        return new ArrayList<>(sources.values());
    }
}
