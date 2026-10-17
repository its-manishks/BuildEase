package com.examly.springapp.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * A user changes the quantity of their own Pending request.
 * version must be the version the user was looking at: if the admin decided the request in the
 * meantime, the server answers 409 instead of editing an approved order.
 */
public record QuantityUpdateRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 10000, message = "Quantity must be at most 10,000 per request")
        Integer quantity,

        @NotNull(message = "Version is required")
        Long version) {
}