package com.examly.springapp.dto.auth;

import com.examly.springapp.model.enums.OtpChannel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Signup verification: the code sent to the email ("Email") or the mobile ("SMS"). */
public record VerifyOtpRequest(
        @NotBlank(message = "Email is required") @Email(message = "Please enter a valid email") String email,
        @NotNull(message = "Channel is required (Email or SMS)") OtpChannel channel,
        @NotBlank(message = "Code is required")
        @Pattern(regexp = AuthValidation.OTP_REGEX, message = AuthValidation.OTP_MESSAGE) String code) {
}