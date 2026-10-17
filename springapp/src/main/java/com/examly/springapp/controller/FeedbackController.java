package com.examly.springapp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.config.SecurityUtils;
import com.examly.springapp.dto.common.MessageResponse;
import com.examly.springapp.dto.common.PageResponse;
import com.examly.springapp.dto.feedback.FeedbackCreateRequest;
import com.examly.springapp.dto.feedback.FeedbackResponse;
import com.examly.springapp.dto.feedback.ReviewableMaterial;
import com.examly.springapp.model.enums.FeedbackCategory;
import com.examly.springapp.service.FeedbackService;

import jakarta.validation.Valid;

/** Reviews API. Role rules live in SecurityConfig; authorship is checked in the service. */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // Posts a review for a material the signed-in user has received (one review per material).
    @PostMapping
    public ResponseEntity<FeedbackResponse> createReview(@Valid @RequestBody FeedbackCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackService.createReview(SecurityUtils.getCurrentUserId(), request));
    }

    // Returns one page of all reviews for admins, filtered by category, star rating or search text.
    @GetMapping
    public ResponseEntity<PageResponse<FeedbackResponse>> getAllReviews(
            @RequestParam(required = false) FeedbackCategory category,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(feedbackService.getAllReviews(category, rating, search, page, size));
    }

    // Lists materials the signed-in user received (shipped) and has not reviewed yet.
    @GetMapping("/awaiting-review")
    public ResponseEntity<List<ReviewableMaterial>> getMaterialsAwaitingReview() {
        return ResponseEntity.ok(feedbackService.getMaterialsAwaitingReview(SecurityUtils.getCurrentUserId()));
    }

    // Returns one review (its author or an admin only).
    @GetMapping("/{feedbackId}")
    public ResponseEntity<FeedbackResponse> getReview(@PathVariable("feedbackId") Long feedbackId) {
        return ResponseEntity.ok(feedbackService.getReview(feedbackId));
    }

    // Returns one page of the signed-in user's own reviews, newest first.
    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<FeedbackResponse>> getMyReviews(
            @PathVariable("userId") Long userId,
            @RequestParam(required = false) FeedbackCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return ResponseEntity.ok(feedbackService.getMyReviews(userId, category, page, size));
    }

    // Deletes the signed-in user's own review.
    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<MessageResponse> deleteReview(@PathVariable("feedbackId") Long feedbackId) {
        return ResponseEntity.ok(feedbackService.deleteReview(feedbackId));
    }
}