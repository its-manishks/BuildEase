package com.examly.springapp.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Starts a password reset: a code is emailed if the account exists (the response never says). */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required") @Email(message = "Please enter a valid email") String email) {
}