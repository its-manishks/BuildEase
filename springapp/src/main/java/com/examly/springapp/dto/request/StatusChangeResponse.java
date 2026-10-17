package com.examly.springapp.dto.request;

import java.time.LocalDateTime;

import com.examly.springapp.model.enums.RequestStatus;

/** One step in a request's timeline. changedBy is the admin's username. */
public record StatusChangeResponse(
        RequestStatus fromStatus,
        RequestStatus toStatus,
        String changedBy,
        String note,
        LocalDateTime changedAt) {
}