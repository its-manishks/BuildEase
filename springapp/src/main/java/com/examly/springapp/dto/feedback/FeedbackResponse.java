package com.examly.springapp.dto.feedback;

import java.time.LocalDate;

import com.examly.springapp.dto.material.MaterialResponse;
import com.examly.springapp.dto.request.UserSummary;
import com.examly.springapp.model.enums.FeedbackCategory;

/**
 * A review as the API returns it (same nested shape as before, plus the rating).
 * materialRequestId / orderedQuantity / orderDate describe the user's own order behind the review:
 * the quantity they ordered and the date they placed it (not current stock, not approval/ship dates).
 * They are null when no matching order exists.
 */
public record FeedbackResponse(
        Long feedbackId,
        String feedbackText,
        Integer rating,
        FeedbackCategory category,
        LocalDate date,
        UserSummary user,
        MaterialResponse material,
        Long materialRequestId,
        Integer orderedQuantity,
        LocalDate orderDate) {
}