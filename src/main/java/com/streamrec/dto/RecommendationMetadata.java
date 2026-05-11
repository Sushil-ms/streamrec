package com.streamrec.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RecommendationMetadata(
        @NotBlank String source,
        @NotBlank String modelVersion,
        @Min(0) long latencyMs
) {
}
