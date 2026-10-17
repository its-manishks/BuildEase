package com.examly.springapp.dto.auth;

import java.time.LocalDateTime;

/** Safe view of an account: never the password, OTPs, lock state or failed-login counter. */
public record UserResponse(
        Long userId,
        String username,
        String email,
        String mobileNumber,
        String address,
        String userRole,
        boolean primaryAdmin,
        boolean emailVerified,
        boolean mobileVerified,
        String accountStatus,
        LocalDateTime createdAt) {
}