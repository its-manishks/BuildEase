package com.examly.springapp.dto.dashboard;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the admin dashboard shows. Every number is computed from the database at request time:
 * no sample or invented statistics. averageRating is null when there are no rated reviews yet.
 */
public record AdminDashboardResponse(
        long activeMaterials,
        long outOfStockMaterials,
        long lowStockMaterials,
        long customers,
        long totalRequests,
        long pendingRequests,
        long shippedRequests,
        long totalReviews,
        Double averageRating,
        List<ChartPoint> requestsByStatus,
        List<ChartPoint> materialsByCategory,
        List<ChartPoint> requestsLast30Days,
        List<ChartPoint> ratingDistribution,
        List<ChartPoint> topRequestedMaterials,
        List<LowStockItem> lowStockItems,
        LocalDateTime generatedAt) {
}