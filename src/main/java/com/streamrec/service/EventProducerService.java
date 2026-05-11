package com.streamrec.service;

import com.streamrec.dto.EventIngestionResponse;
import com.streamrec.dto.UserMusicEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventProducerService {

    public static final String USER_MUSIC_EVENTS_TOPIC = "user-music-events";
    private static final String ACCEPTED_STATUS = "ACCEPTED";

    private final KafkaTemplate<String, UserMusicEvent> kafkaTemplate;

    public EventProducerService(KafkaTemplate<String, UserMusicEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public EventIngestionResponse publishEvent(UserMusicEvent userMusicEvent) {
        kafkaTemplate.send(USER_MUSIC_EVENTS_TOPIC, userMusicEvent.userId(), userMusicEvent);

        return new EventIngestionResponse(
                ACCEPTED_STATUS,
                USER_MUSIC_EVENTS_TOPIC,
                userMusicEvent.userId(),
                userMusicEvent.trackId(),
                userMusicEvent.eventType()
        );
    }
}
