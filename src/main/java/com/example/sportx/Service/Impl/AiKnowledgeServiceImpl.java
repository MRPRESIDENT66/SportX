package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.sportx.Entity.AiKnowledgeDocument;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Mapper.ChallengeMapper;
import com.example.sportx.Mapper.SpotsMapper;
import com.example.sportx.Service.AiKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiKnowledgeServiceImpl implements AiKnowledgeService {

    private static final String RULES_RESOURCE_PATH = "ai/platform-rules.md";

    private final SpotsMapper spotsMapper;
    private final ChallengeMapper challengeMapper;

    @Override
    public List<AiKnowledgeDocument> loadAllKnowledgeDocuments() {
        List<AiKnowledgeDocument> documents = new ArrayList<>();
        documents.addAll(loadSpotDocuments());
        documents.addAll(loadChallengeDocuments());
        documents.addAll(loadRuleDocuments());
        return documents;
    }

    private List<AiKnowledgeDocument> loadSpotDocuments() {
        List<Spots> spots = spotsMapper.selectList(new LambdaQueryWrapper<>());
        List<AiKnowledgeDocument> documents = new ArrayList<>();
        for (Spots spot : spots) {
            if (spot == null || spot.getId() == null) {
                continue;
            }
            AiKnowledgeDocument document = new AiKnowledgeDocument();
            document.setId("spot-" + spot.getId());
            document.setType("spot");
            document.setTitle(spot.getName());
            document.setContent(buildSpotContent(spot));
            document.setSource("spots:" + spot.getId());
            documents.add(document);
        }
        return documents;
    }

    private List<AiKnowledgeDocument> loadChallengeDocuments() {
        List<Challenge> challenges = challengeMapper.selectList(new LambdaQueryWrapper<>());
        List<AiKnowledgeDocument> documents = new ArrayList<>();
        for (Challenge challenge : challenges) {
            if (challenge == null || challenge.getId() == null) {
                continue;
            }
            AiKnowledgeDocument document = new AiKnowledgeDocument();
            document.setId("challenge-" + challenge.getId());
            document.setType("challenge");
            document.setTitle(challenge.getChallengeName());
            document.setContent(buildChallengeContent(challenge));
            document.setSource("challenge:" + challenge.getId());
            documents.add(document);
        }
        return documents;
    }

    private List<AiKnowledgeDocument> loadRuleDocuments() {
        String content = readRulesFile();
        if (content == null || content.isBlank()) {
            return List.of();
        }
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setId("rule-platform");
        document.setType("rule");
        document.setTitle("SportX Platform Rules");
        document.setContent(content);
        document.setSource(RULES_RESOURCE_PATH);
        return List.of(document);
    }

    private String buildSpotContent(Spots spot) {
        return """
                Spot Name: %s
                Spot Type: %s
                Region: %s
                Address: %s
                Phone: %s
                Description: %s
                Rating: %s
                Visit Count: %s
                Open Status: %s
                Open Time: %s
                """.formatted(
                safeText(spot.getName()),
                safeText(spot.getType()),
                safeText(spot.getRegion()),
                safeText(spot.getAddress()),
                safeText(spot.getPhone()),
                safeText(spot.getDescription()),
                spot.getRating() == null ? "unknown" : spot.getRating(),
                spot.getVisitCount() == null ? "unknown" : spot.getVisitCount(),
                spot.getIsOpen() == null ? "unknown" : (spot.getIsOpen() ? "open" : "closed"),
                safeText(spot.getOpenTime())
        ).trim();
    }

    private String buildChallengeContent(Challenge challenge) {
        return """
                Challenge Name: %s
                Description: %s
                Spot Id: %s
                Total Slots: %s
                Joined Slots: %s
                Start Date: %s
                End Date: %s
                """.formatted(
                safeText(challenge.getChallengeName()),
                safeText(challenge.getDescription()),
                challenge.getSpotId() == null ? "unknown" : challenge.getSpotId(),
                challenge.getTotalSlots() == null ? "unknown" : challenge.getTotalSlots(),
                challenge.getJoinedSlots() == null ? "unknown" : challenge.getJoinedSlots(),
                challenge.getStartTime() == null ? "unknown" : challenge.getStartTime(),
                challenge.getEndTime() == null ? "unknown" : challenge.getEndTime()
        ).trim();
    }

    private String readRulesFile() {
        ClassPathResource resource = new ClassPathResource(RULES_RESOURCE_PATH);
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read AI rules resource", exception);
        }
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
