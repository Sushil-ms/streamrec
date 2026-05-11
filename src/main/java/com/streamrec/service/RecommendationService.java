package com.streamrec.service;

import com.streamrec.dto.RecommendationItem;
import com.streamrec.dto.RecommendationMetadata;
import com.streamrec.dto.RecommendationResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    public RecommendationResponse getRecommendations(String userId) {
        RecommendationItem item = new RecommendationItem(
                "track_456",
                "Midnight Drive",
                "Nova Lane",
                "Synthwave",
                BigDecimal.valueOf(0.91),
                "Because you recently played similar Synthwave tracks"
        );

        RecommendationMetadata metadata = new RecommendationMetadata(
                "mock",
                "v1",
                0L
        );

        return new RecommendationResponse(
                userId,
                Instant.parse("2026-05-04T16:30:00Z"),
                List.of(item),
                metadata
        );
    }
}
