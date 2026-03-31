package com.example.sportx.Service.Impl;

import com.example.sportx.Config.AiRagProperties;
import com.example.sportx.Entity.AiKnowledgeChunk;
import com.example.sportx.Service.AiGenerationService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiGenerationServiceImpl implements AiGenerationService {

    private final AiRagProperties aiRagProperties;

    @Override
    public boolean isConfigured() {
        return hasText(aiRagProperties.getChatApiKey()) && hasText(aiRagProperties.getChatModel());
    }

    @Override
    public String generateAnswer(String question, List<AiKnowledgeChunk> chunks) {
        if (!isConfigured()) {
            throw new IllegalStateException("AI chat model is not configured");
        }
        if (!hasText(question)) {
            return "";
        }
        if (chunks == null || chunks.isEmpty()) {
            return "I could not find enough SportX knowledge to answer this question.";
        }

        ChatResponse response = chatModel().chat(List.of(
                SystemMessage.from(buildSystemPrompt()),
                UserMessage.from(buildUserPrompt(question, chunks))
        ));

        if (response == null || response.aiMessage() == null || !hasText(response.aiMessage().text())) {
            return "The AI model returned an empty answer.";
        }
        return response.aiMessage().text().trim();
    }

    private OpenAiChatModel chatModel() {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(aiRagProperties.getChatApiKey())
                .modelName(aiRagProperties.getChatModel())
                .temperature(aiRagProperties.getChatTemperature())
                .maxCompletionTokens(aiRagProperties.getChatMaxTokens());
        if (hasText(aiRagProperties.getChatBaseUrl())) {
            builder.baseUrl(aiRagProperties.getChatBaseUrl());
        }
        return builder.build();
    }

    private String buildSystemPrompt() {
        return """
                You are the SportX AI assistant.
                Answer only based on the provided SportX knowledge snippets.
                If the snippets are insufficient, say that the current knowledge base does not contain enough information.
                Do not invent facts, numbers, dates, or platform rules.
                Keep the answer concise and practical.
                """.trim();
    }

    private String buildUserPrompt(String question, List<AiKnowledgeChunk> chunks) {
        StringBuilder builder = new StringBuilder();
        builder.append("User question:\n")
                .append(question.trim())
                .append("\n\n")
                .append("Relevant SportX knowledge:\n");

        int maxChunks = Math.min(4, chunks.size());
        for (int i = 0; i < maxChunks; i++) {
            AiKnowledgeChunk chunk = chunks.get(i);
            builder.append("\n[")
                    .append(i + 1)
                    .append("] ")
                    .append(chunk.getTitle() == null ? "Untitled" : chunk.getTitle())
                    .append(" (type=")
                    .append(chunk.getType() == null ? "unknown" : chunk.getType())
                    .append(", source=")
                    .append(chunk.getSource() == null ? "unknown" : chunk.getSource())
                    .append(")\n")
                    .append(chunk.getContent());
        }

        builder.append("\n\nAnswer the user question using only the knowledge above.");
        return builder.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
