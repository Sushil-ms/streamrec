package com.streamrec.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record UserMusicEvent(
        @NotBlank String userId,
        @NotBlank String trackId,
        @NotNull EventType eventType,
        @NotNull Instant timestamp
) {
}
