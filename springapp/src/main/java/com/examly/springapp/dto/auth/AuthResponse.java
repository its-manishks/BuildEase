package com.examly.springapp.dto.auth;

/** Successful login (or password change): the token plus what the navbar and guards need. */
public record AuthResponse(
        String token,
        Long userId,
        String username,
        String email,
        String userRole,
        boolean mustChangePassword,
        boolean primaryAdmin) {
}