package com.streamrec.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RecommendationItem(
        @NotBlank String trackId,
        @NotBlank String title,
        @NotBlank String artist,
        @NotBlank String genre,
        @NotNull @DecimalMin("0.0") BigDecimal score,
        @NotBlank String reason
) {
}
