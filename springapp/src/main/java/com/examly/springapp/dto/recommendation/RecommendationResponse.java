package com.examly.springapp.dto.recommendation;

import java.util.List;

/**
 * Recommendations for the signed-in customer. personalized is true only when the list is based on
 * their own purchase history; otherwise it shows popular/available materials and the UI says so.
 */
public record RecommendationResponse(boolean personalized, List<RecommendationItem> items) {
}