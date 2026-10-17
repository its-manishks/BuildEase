package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.common.MessageResponse;
import com.examly.springapp.dto.common.PageResponse;
import com.examly.springapp.dto.feedback.FeedbackCreateRequest;
import com.examly.springapp.dto.feedback.FeedbackResponse;
import com.examly.springapp.dto.feedback.ReviewableMaterial;
import com.examly.springapp.model.enums.FeedbackCategory;

/** Verified reviews: only after shipping, one per user per material. */
public interface FeedbackService {

    FeedbackResponse createReview(Long userId, FeedbackCreateRequest request);

    FeedbackResponse getReview(Long feedbackId);

    PageResponse<FeedbackResponse> getMyReviews(Long userId, FeedbackCategory category, int page, int size);

    PageResponse<FeedbackResponse> getAllReviews(FeedbackCategory category, Integer rating, String search,
                                                 int page, int size);

    List<ReviewableMaterial> getMaterialsAwaitingReview(Long userId);

    MessageResponse deleteReview(Long feedbackId);
}