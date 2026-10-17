package com.examly.springapp.dto.feedback;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Only the review text is sent: no user, material or order information. */
public record SpellCheckRequest(
        @NotNull(message = "Text is required")
        @Size(max = 1000, message = "Text must be at most 1000 characters")
        String text) {
}