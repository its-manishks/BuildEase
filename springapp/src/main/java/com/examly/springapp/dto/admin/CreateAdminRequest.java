package com.examly.springapp.dto.admin;

import com.examly.springapp.dto.auth.AuthValidation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The primary admin creates another admin with a temporary password. The new admin must change it
 * at first login (same server-side enforcement as the seeded admin).
 */
public record CreateAdminRequest(
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

        @NotBlank(message = "Temporary password is required")
        @Pattern(regexp = AuthValidation.PASSWORD_REGEX, message = AuthValidation.PASSWORD_MESSAGE)
        String temporaryPassword) {
}