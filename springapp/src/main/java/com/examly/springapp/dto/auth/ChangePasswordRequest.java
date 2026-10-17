package com.examly.springapp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** A logged-in user changes their password (mandatory for the seeded admin's first login). */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String currentPassword,
        @NotBlank(message = "New password is required")
        @Pattern(regexp = AuthValidation.PASSWORD_REGEX, message = AuthValidation.PASSWORD_MESSAGE) String newPassword) {
}