package com.examly.springapp.dto.request;

/**
 * Per-user request statistics for the admin insights page. Built directly by an aggregate
 * database query (COUNT / SUM ... GROUP BY), not by loading every request into memory.
 */
public record UserInsightResponse(
        Long userId,
        String username,
        String email,
        Long totalRequests,
        Long highUrgencyRequests,
        Long pendingRequests) {
}