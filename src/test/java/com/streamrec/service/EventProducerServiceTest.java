package com.streamrec.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.streamrec.dto.EventIngestionResponse;
import com.streamrec.dto.EventType;
import com.streamrec.dto.UserMusicEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class EventProducerServiceTest {

    @Mock
    private KafkaTemplate<String, UserMusicEvent> kafkaTemplate;

    @Test
    void publishEventSendsExpectedKafkaMessage() {
        EventProducerService eventProducerService = new EventProducerService(kafkaTemplate);
        UserMusicEvent userMusicEvent = new UserMusicEvent(
                "user_123",
                "track_456",
                EventType.PLAY,
                Instant.parse("2026-05-10T12:00:00Z")
        );

        EventIngestionResponse response = eventProducerService.publishEvent(userMusicEvent);

        verify(kafkaTemplate).send(EventProducerService.USER_MUSIC_EVENTS_TOPIC, "user_123", userMusicEvent);
        assertThat(response).isEqualTo(new EventIngestionResponse(
                "ACCEPTED",
                EventProducerService.USER_MUSIC_EVENTS_TOPIC,
                "user_123",
                "track_456",
                EventType.PLAY
        ));
    }
}
