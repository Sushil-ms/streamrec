package com.streamrec.dto;

public record EventIngestionResponse(
        String status,
        String topic,
        String userId,
        String trackId,
        EventType eventType
) {
}
