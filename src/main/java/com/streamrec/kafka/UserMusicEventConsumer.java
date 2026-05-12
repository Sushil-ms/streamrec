package com.streamrec.kafka;

import com.streamrec.dto.UserMusicEvent;
import com.streamrec.service.EventProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserMusicEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserMusicEventConsumer.class);

    @KafkaListener(topics = EventProducerService.USER_MUSIC_EVENTS_TOPIC)
    public void consume(UserMusicEvent userMusicEvent) {
        if (!StringUtils.hasText(userMusicEvent.userId())
                || !StringUtils.hasText(userMusicEvent.trackId())) {
            log.warn(
                    "Skipping user music event with missing identifiers. userId={}, trackId={}, eventType={}, timestamp={}",
                    userMusicEvent.userId(),
                    userMusicEvent.trackId(),
                    userMusicEvent.eventType(),
                    userMusicEvent.timestamp()
            );
            return;
        }

        log.info(
                "Consumed user music event. userId={}, trackId={}, eventType={}, timestamp={}",
                userMusicEvent.userId(),
                userMusicEvent.trackId(),
                userMusicEvent.eventType(),
                userMusicEvent.timestamp()
        );
    }
}
