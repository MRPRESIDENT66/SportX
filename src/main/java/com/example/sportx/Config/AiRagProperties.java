package com.example.sportx.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.rag")
public class AiRagProperties {
    private String embeddingApiKey;
    private String embeddingBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String embeddingModel = "text-embedding-v4";
    private String chatApiKey;
    private String chatBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String chatModel = "qwen-plus";
    private Double chatTemperature = 0.2;
    private Integer chatMaxTokens = 600;
    private Integer maxResults = 4;
    private Double minScore = 0.6;
}
