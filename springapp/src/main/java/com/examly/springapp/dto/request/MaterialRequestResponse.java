package com.examly.springapp.dto.request;

import java.time.LocalDate;

import com.examly.springapp.dto.material.MaterialResponse;
import com.examly.springapp.model.enums.RequestStatus;
import com.examly.springapp.model.enums.TimeSlot;
import com.examly.springapp.model.enums.UrgencyLevel;

/**
 * A material request as the API returns it. Same nested shape as before (material, user) so the
 * existing screens keep working, plus totalPrice and version.
 */
public record MaterialRequestResponse(
        Long materialRequestId,
        int quantity,
        UrgencyLevel urgencyLevel,
        LocalDate requestDate,
        LocalDate preferredDeliveryDate,
        TimeSlot timeSlot,
        RequestStatus status,
        String deliveryAddress,
        String contactNumber,
        String comments,
        double totalPrice,
        Long version,
        MaterialResponse material,
        UserSummary user) {
}