package com.example.sportx.Controller;

import com.example.sportx.Entity.dto.AiAskRequestDto;
import com.example.sportx.Entity.vo.AiAskResponseVo;
import com.example.sportx.Entity.vo.AiReindexResponseVo;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.AiRetrievalService;
import com.example.sportx.Service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Validated
@Tag(name = "AI", description = "AI assistant APIs")
public class AiController {

    private final AiService aiService;
    private final AiRetrievalService aiRetrievalService;

    @PostMapping("/ask")
    @Operation(summary = "Ask AI assistant", description = "Ask a platform knowledge question and get answer with sources")
    public Result<AiAskResponseVo> ask(@Valid @RequestBody AiAskRequestDto requestDto) {
        // AI ask endpoint: implementation is delegated to the service layer.
        return aiService.ask(requestDto);
    }

    @PostMapping("/reindex")
    @Operation(summary = "Rebuild AI index", description = "Reload platform knowledge, regenerate embeddings, and rebuild the in-memory vector index")
    public Result<AiReindexResponseVo> reindex() {
        if (!aiRetrievalService.isConfigured()) {
            return Result.error("AI embedding is not configured. Please set ai.rag.embedding-api-key first");
        }

        int indexedChunkCount = aiRetrievalService.rebuildIndex();
        AiReindexResponseVo responseVo = new AiReindexResponseVo();
        responseVo.setIndexedChunkCount(indexedChunkCount);
        return Result.success(responseVo);
    }
}
