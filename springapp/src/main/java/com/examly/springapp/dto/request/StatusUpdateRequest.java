package com.examly.springapp.dto.request;

import com.examly.springapp.model.enums.RequestStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin decision on a request.
 *  - version must be the version the admin was looking at: if someone else changed the request in
 *    the meantime, the server answers 409 instead of overwriting
 *  - note is shown to the customer in the request's timeline; it is REQUIRED when rejecting
 *    (the customer deserves a reason) and optional otherwise (e.g. delivery details when shipping)
 */
public record StatusUpdateRequest(
        @NotNull(message = "Status is required (Approved, Rejected or Shipped)")
        RequestStatus status,

        @NotNull(message = "Version is required")
        Long version,

        @Size(max = 300, message = "The note must be at most 300 characters")
        String note) {
}