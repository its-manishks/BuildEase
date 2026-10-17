package com.examly.springapp.dto.request;

/** The requester, as admins see them next to a request (never the password or account internals). */
public record UserSummary(Long userId, String username, String email, String mobileNumber) {
}