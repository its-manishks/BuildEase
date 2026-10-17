package com.examly.springapp.dto.dashboard;

/** The signed-in user's own summary for the Home page. */
public record UserDashboardResponse(
        long totalRequests,
        long pendingRequests,
        long approvedRequests,
        long shippedRequests,
        long rejectedRequests,
        long reviewsWritten,
        long awaitingReview) {
}