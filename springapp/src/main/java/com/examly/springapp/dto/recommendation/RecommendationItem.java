package com.examly.springapp.dto.recommendation;

import com.examly.springapp.dto.material.MaterialResponse;

/** A recommended material and the honest reason it is shown (e.g. "Often needed with Cement"). */
public record RecommendationItem(MaterialResponse material, String reason) {
}