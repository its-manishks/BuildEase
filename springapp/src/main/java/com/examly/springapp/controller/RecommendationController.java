package com.examly.springapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.config.SecurityUtils;
import com.examly.springapp.dto.recommendation.RecommendationResponse;
import com.examly.springapp.service.recommendation.RecommendationService;

/** Material recommendations for the signed-in customer (User role; identity from the token only). */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    // Returns materials recommended from the signed-in customer's own purchase history (or popular ones).
    @GetMapping
    public ResponseEntity<RecommendationResponse> forMe(@RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(recommendationService.forUser(SecurityUtils.getCurrentUserId(), limit));
    }

    // Returns materials related to the one just ordered.
    @GetMapping("/after-order/{materialId}")
    public ResponseEntity<RecommendationResponse> afterOrder(@PathVariable("materialId") Long materialId,
                                                             @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(recommendationService.afterOrder(SecurityUtils.getCurrentUserId(), materialId, limit));
    }
}