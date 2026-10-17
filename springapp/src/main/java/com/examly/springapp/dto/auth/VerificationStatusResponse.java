package com.examly.springapp.dto.auth;

/** Where signup verification stands after a code was accepted. */
public record VerificationStatusResponse(
        boolean emailVerified,
        boolean mobileVerified,
        String accountStatus,
        String message) {
}