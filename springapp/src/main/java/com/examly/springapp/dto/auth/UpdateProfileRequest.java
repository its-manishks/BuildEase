package com.examly.springapp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Profile changes a user may make without re-verification: the saved delivery address. */
public record UpdateProfileRequest(
        @NotBlank(message = "Address is required")
        @Size(min = 10, max = 300, message = "Address must be 10-300 characters") String address) {
}