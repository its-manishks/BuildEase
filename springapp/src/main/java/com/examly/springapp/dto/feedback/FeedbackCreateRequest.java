package com.examly.springapp.dto.feedback;

import com.examly.springapp.model.enums.FeedbackCategory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A review. The user comes from the token, the date and the linked order are set by the server. */
public record FeedbackCreateRequest(
        @NotNull(message = "Material is required")
        Long materialId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5 stars")
        @Max(value = 5, message = "Rating must be between 1 and 5 stars")
        Integer rating,

        /** Optional: the form no longer asks for it; the server stores "Other" when it is missing. */
        FeedbackCategory category,

        @NotBlank(message = "Feedback text is required")
        @Size(min = 10, max = 1000, message = "Feedback must be 10-1000 characters")
        String feedbackText) {
}