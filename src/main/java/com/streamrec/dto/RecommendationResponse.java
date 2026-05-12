package com.streamrec.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record RecommendationResponse(
        @NotBlank String userId,
        @NotNull Instant generatedAt,
        @NotNull List<@Valid RecommendationItem> recommendations,
        @NotNull @Valid RecommendationMetadata metadata
) {
}
