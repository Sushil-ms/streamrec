package com.streamrec.kafka;

import com.streamrec.dto.UserMusicEvent;
import com.streamrec.service.EventProducerService;
import com.streamrec.service.RecommendationScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserMusicEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserMusicEventConsumer.class);

    private final RecommendationScoringService recommendationScoringService;

    public UserMusicEventConsumer(RecommendationScoringService recommendationScoringService) {
        this.recommendationScoringService = recommendationScoringService;
    }

    @KafkaListener(topics = EventProducerService.USER_MUSIC_EVENTS_TOPIC)
    public void consume(UserMusicEvent userMusicEvent) {
        if (userMusicEvent == null) {
            log.warn("Skipping null user music event from Kafka topic={}", EventProducerService.USER_MUSIC_EVENTS_TOPIC);
            return;
        }

        if (!StringUtils.hasText(userMusicEvent.userId())
                || !StringUtils.hasText(userMusicEvent.trackId())
                || userMusicEvent.eventType() == null) {
            log.warn(
                    "Skipping malformed user music event. userId={}, trackId={}, eventType={}, timestamp={}",
                    userMusicEvent.userId(),
                    userMusicEvent.trackId(),
                    userMusicEvent.eventType(),
                    userMusicEvent.timestamp()
            );
            return;
        }

        log.info(
                "Accepted user music event for scoring. userId={}, trackId={}, eventType={}, timestamp={}",
                userMusicEvent.userId(),
                userMusicEvent.trackId(),
                userMusicEvent.eventType(),
                userMusicEvent.timestamp()
        );

        try {
            recommendationScoringService.processEvent(userMusicEvent);
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "Skipping invalid user music event during scoring. userId={}, trackId={}, eventType={}, error={}",
                    userMusicEvent.userId(),
                    userMusicEvent.trackId(),
                    userMusicEvent.eventType(),
                    exception.getMessage()
            );
        } catch (Exception exception) {
            log.error(
                    "Failed to process user music event for recommendation scoring. userId={}, trackId={}, eventType={}",
                    userMusicEvent.userId(),
                    userMusicEvent.trackId(),
                    userMusicEvent.eventType(),
                    exception
            );
        }
    }
}
