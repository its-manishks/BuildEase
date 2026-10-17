package com.examly.springapp.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Helper used by every service to find out who is calling.
 * Example: Long currentUserId = SecurityUtils.getCurrentUserId();
 */
public final class SecurityUtils {

    private static final String ADMIN_ROLE = "Admin";

    private SecurityUtils() {
        // Utility class
    }

    public static UserPrinciple getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrinciple principal) {
            return principal;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static boolean isAdmin() {
        return ADMIN_ROLE.equals(getCurrentUser().getRole());
    }
}