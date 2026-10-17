package com.examly.springapp.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Completes a password reset with the emailed code. */
public record ResetPasswordRequest(
        @NotBlank(message = "Email is required") @Email(message = "Please enter a valid email") String email,
        @NotBlank(message = "Code is required")
        @Pattern(regexp = AuthValidation.OTP_REGEX, message = AuthValidation.OTP_MESSAGE) String code,
        @NotBlank(message = "New password is required")
        @Pattern(regexp = AuthValidation.PASSWORD_REGEX, message = AuthValidation.PASSWORD_MESSAGE) String newPassword) {
}