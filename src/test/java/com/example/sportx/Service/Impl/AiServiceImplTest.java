package com.example.sportx.Service.Impl;

import com.example.sportx.Entity.AiKnowledgeChunk;
import com.example.sportx.Entity.dto.AiAskRequestDto;
import com.example.sportx.Entity.vo.AiAskResponseVo;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.AiGenerationService;
import com.example.sportx.Service.AiRetrievalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private AiRetrievalService aiRetrievalService;

    @Mock
    private AiGenerationService aiGenerationService;

    @InjectMocks
    private AiServiceImpl aiService;

    @Test
    void shouldReturnErrorWhenEmbeddingIsNotConfigured() {
        AiAskRequestDto requestDto = new AiAskRequestDto();
        requestDto.setQuestion("What challenges are active now?");

        when(aiRetrievalService.isConfigured()).thenReturn(false);

        Result<AiAskResponseVo> result = aiService.ask(requestDto);

        assertThat(result.getCode()).isEqualTo(1);
        assertThat(result.getMessage()).contains("not configured");
        assertThat(result.getData()).isNull();
    }

    @Test
    void shouldReturnErrorWhenChatModelIsNotConfigured() {
        AiAskRequestDto requestDto = new AiAskRequestDto();
        requestDto.setQuestion("What challenges are active now?");

        when(aiRetrievalService.isConfigured()).thenReturn(true);
        when(aiGenerationService.isConfigured()).thenReturn(false);

        Result<AiAskResponseVo> result = aiService.ask(requestDto);

        assertThat(result.getCode()).isEqualTo(1);
        assertThat(result.getMessage()).contains("chat model");
        assertThat(result.getData()).isNull();
    }

    @Test
    void shouldReturnErrorWhenNoRelevantKnowledgeWasFound() {
        AiAskRequestDto requestDto = new AiAskRequestDto();
        requestDto.setQuestion("What challenges are active now?");

        when(aiRetrievalService.isConfigured()).thenReturn(true);
        when(aiGenerationService.isConfigured()).thenReturn(true);
        when(aiRetrievalService.searchRelevantChunks(requestDto.getQuestion())).thenReturn(List.of());

        Result<AiAskResponseVo> result = aiService.ask(requestDto);

        assertThat(result.getCode()).isEqualTo(1);
        assertThat(result.getMessage()).contains("No relevant knowledge");
        assertThat(result.getData()).isNull();
    }

    @Test
    void shouldReturnRetrievalAnswerAndDeduplicatedSources() {
        AiAskRequestDto requestDto = new AiAskRequestDto();
        requestDto.setQuestion("Tell me about Manchester stadiums");

        AiKnowledgeChunk firstChunk = buildChunk("spot-2-chunk-0", "spot-2", "spot", "Etihad Stadium", "Etihad is a large stadium in Manchester.");
        AiKnowledgeChunk duplicateDocumentChunk = buildChunk("spot-2-chunk-1", "spot-2", "spot", "Etihad Stadium", "It is highly rated for football.");
        AiKnowledgeChunk secondChunk = buildChunk("spot-5-chunk-0", "spot-5", "spot", "Old Trafford", "Old Trafford is another major stadium in Manchester.");

        when(aiRetrievalService.isConfigured()).thenReturn(true);
        when(aiGenerationService.isConfigured()).thenReturn(true);
        when(aiRetrievalService.searchRelevantChunks(requestDto.getQuestion()))
                .thenReturn(List.of(firstChunk, duplicateDocumentChunk, secondChunk));
        when(aiGenerationService.generateAnswer(requestDto.getQuestion(), List.of(firstChunk, duplicateDocumentChunk, secondChunk)))
                .thenReturn("Manchester has Etihad Stadium and Old Trafford as key football venues.");

        Result<AiAskResponseVo> result = aiService.ask(requestDto);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getAnswer()).contains("Etihad Stadium");
        assertThat(result.getData().getSources()).hasSize(2);
        assertThat(result.getData().getSources())
                .extracting("id")
                .containsExactly("spot-2", "spot-5");
    }

    private AiKnowledgeChunk buildChunk(String chunkId, String documentId, String type, String title, String content) {
        AiKnowledgeChunk chunk = new AiKnowledgeChunk();
        chunk.setChunkId(chunkId);
        chunk.setDocumentId(documentId);
        chunk.setType(type);
        chunk.setTitle(title);
        chunk.setContent(content);
        chunk.setSource(type + ":" + documentId);
        return chunk;
    }
}
