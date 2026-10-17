package com.examly.springapp.dto.material;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Restock (positive) or correction (negative). Stock can never end up below zero. */
public record StockAdjustRequest(
        @NotNull(message = "Quantity change is required")
        @Min(value = -1000000, message = "Quantity change is too large")
        @Max(value = 1000000, message = "Quantity change is too large")
        Integer delta) {
}