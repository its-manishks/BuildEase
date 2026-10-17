package com.examly.springapp.mapper;

import com.examly.springapp.dto.feedback.FeedbackResponse;
import com.examly.springapp.dto.feedback.ReviewableMaterial;
import com.examly.springapp.dto.request.UserSummary;
import com.examly.springapp.model.Feedback;
import com.examly.springapp.model.Material;
import com.examly.springapp.model.MaterialRequest;
import com.examly.springapp.model.User;

/** Feedback entity → API response. */
public final class FeedbackMapper {

    private FeedbackMapper() {
        // Utility class
    }

    public static FeedbackResponse toResponse(Feedback feedback) {
        return toResponse(feedback, feedback.getMaterialRequest());
    }

    /** order = the user's request behind this review (may be null when none can be found). */
    public static FeedbackResponse toResponse(Feedback feedback, MaterialRequest order) {
        User user = feedback.getUser();
        Material material = feedback.getMaterial();
        return new FeedbackResponse(
                feedback.getFeedbackId(),
                feedback.getFeedbackText(),
                feedback.getRating(),
                feedback.getCategory(),
                feedback.getDate(),
                user == null ? null : new UserSummary(user.getUserId(), user.getUsername(), user.getEmail(),
                        user.getMobileNumber()),
                material == null ? null : MaterialMapper.toResponse(material),
                order == null ? null : order.getMaterialRequestId(),
                order == null ? null : order.getQuantity(),
                order == null ? null : order.getRequestDate());
    }

    public static ReviewableMaterial toReviewable(Material material) {
        return new ReviewableMaterial(material.getMaterialId(), material.getMaterialName(), material.getCategory(),
                MaterialMapper.toResponse(material).imageUrl());
    }
}