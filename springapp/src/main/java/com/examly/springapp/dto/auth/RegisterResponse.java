package com.examly.springapp.dto.auth;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Result of signup: the account exists but must be verified.
 * failedChannels lists codes that could not be delivered (the user can press Resend).
 * devOtps is present ONLY when app.otp.delivery-mode=dev (testing without SMTP/SMS).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegisterResponse(
        Long userId,
        String email,
        String maskedEmail,
        String maskedMobile,
        String accountStatus,
        List<String> failedChannels,
        Map<String, String> devOtps) {
}