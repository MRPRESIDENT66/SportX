package com.example.sportx.Entity;

import lombok.Data;

@Data
public class AiKnowledgeChunk {
    private String chunkId;
    private String documentId;
    private String type;
    private String title;
    private String content;
    private String source;
    private Integer chunkIndex;
}
