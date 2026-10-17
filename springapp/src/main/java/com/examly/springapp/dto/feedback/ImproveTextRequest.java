package com.examly.springapp.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The review text to reword (only the text: no user, material or order information). */
public record ImproveTextRequest(
        @NotBlank(message = "Text is required")
        @Size(min = 10, max = 1000, message = "Text must be 10-1000 characters")
        String text) {
}