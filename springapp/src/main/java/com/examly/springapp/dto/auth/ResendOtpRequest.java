package com.examly.springapp.dto.auth;

import com.examly.springapp.model.enums.OtpChannel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Asks for a new signup verification code on one channel. */
public record ResendOtpRequest(
        @NotBlank(message = "Email is required") @Email(message = "Please enter a valid email") String email,
        @NotNull(message = "Channel is required (Email or SMS)") OtpChannel channel) {
}