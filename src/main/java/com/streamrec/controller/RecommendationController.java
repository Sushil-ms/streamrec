package com.streamrec.controller;

import com.streamrec.dto.RecommendationResponse;
import com.streamrec.dto.StandardApiResponse;
import com.streamrec.service.RecommendationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{userId}")
    public StandardApiResponse<RecommendationResponse> getRecommendations(
            @PathVariable @NotBlank String userId) {
        RecommendationResponse response = recommendationService.getRecommendations(userId);
        return StandardApiResponse.success(response);
    }
}
