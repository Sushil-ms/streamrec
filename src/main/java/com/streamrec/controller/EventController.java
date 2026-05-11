package com.streamrec.controller;

import com.streamrec.dto.EventIngestionResponse;
import com.streamrec.dto.StandardApiResponse;
import com.streamrec.dto.UserMusicEvent;
import com.streamrec.service.EventProducerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventProducerService eventProducerService;

    public EventController(EventProducerService eventProducerService) {
        this.eventProducerService = eventProducerService;
    }

    @PostMapping
    public StandardApiResponse<EventIngestionResponse> ingestEvent(
            @Valid @RequestBody UserMusicEvent userMusicEvent) {
        EventIngestionResponse response = eventProducerService.publishEvent(userMusicEvent);
        return StandardApiResponse.success(response);
    }
}
