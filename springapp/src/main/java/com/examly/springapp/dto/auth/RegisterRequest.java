package com.examly.springapp.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Signup input. There is deliberately NO role field: every signup becomes a User.
 * Unknown JSON fields (e.g. a forged "userRole") are ignored by Jackson.
 */
public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Pattern(regexp = AuthValidation.USERNAME_REGEX, message = AuthValidation.USERNAME_MESSAGE)
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = AuthValidation.MOBILE_REGEX, message = AuthValidation.MOBILE_MESSAGE)
        String mobileNumber,

        @NotBlank(message = "Password is required")
        @Pattern(regexp = AuthValidation.PASSWORD_REGEX, message = AuthValidation.PASSWORD_MESSAGE)
        String password,

        @NotBlank(message = "Address is required")
        @Size(min = 10, max = 300, message = "Address must be 10-300 characters")
        String address) {
}