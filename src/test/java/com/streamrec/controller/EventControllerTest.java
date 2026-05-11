package com.streamrec.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamrec.dto.EventIngestionResponse;
import com.streamrec.dto.EventType;
import com.streamrec.dto.UserMusicEvent;
import com.streamrec.service.EventProducerService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventProducerService eventProducerService;

    @Test
    void ingestEventDelegatesToProducerServiceAndReturnsAcceptedResponse() throws Exception {
        UserMusicEvent userMusicEvent = new UserMusicEvent(
                "user_123",
                "track_456",
                EventType.PLAY,
                Instant.parse("2026-05-10T12:00:00Z")
        );
        EventIngestionResponse response = new EventIngestionResponse(
                "ACCEPTED",
                EventProducerService.USER_MUSIC_EVENTS_TOPIC,
                "user_123",
                "track_456",
                EventType.PLAY
        );

        when(eventProducerService.publishEvent(userMusicEvent)).thenReturn(response);

        mockMvc.perform(post("/api/events")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userMusicEvent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.topic").value("user-music-events"))
                .andExpect(jsonPath("$.data.userId").value("user_123"))
                .andExpect(jsonPath("$.data.trackId").value("track_456"))
                .andExpect(jsonPath("$.data.eventType").value("PLAY"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(eventProducerService).publishEvent(userMusicEvent);
    }
}
