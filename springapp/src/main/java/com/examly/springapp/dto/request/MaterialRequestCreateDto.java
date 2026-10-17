package com.examly.springapp.dto.request;

import java.time.LocalDate;

import com.examly.springapp.dto.auth.AuthValidation;
import com.examly.springapp.model.enums.TimeSlot;
import com.examly.springapp.model.enums.UrgencyLevel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What a user sends to request a material. The server sets the user (from the token), the request
 * date and the status (Pending). Address and contact are optional: when left empty, the saved
 * profile address and mobile number are used.
 */
public record MaterialRequestCreateDto(
        @NotNull(message = "Material is required")
        Long materialId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 10000, message = "Quantity must be at most 10,000 per request")
        Integer quantity,

        @NotNull(message = "Urgency level is required")
        UrgencyLevel urgencyLevel,

        @NotNull(message = "Preferred delivery date is required")
        LocalDate preferredDeliveryDate,

        @NotNull(message = "Time slot is required")
        TimeSlot timeSlot,

        @Size(min = 10, max = 300, message = "Delivery address must be 10-300 characters")
        String deliveryAddress,

        @Pattern(regexp = AuthValidation.MOBILE_REGEX, message = AuthValidation.MOBILE_MESSAGE)
        String contactNumber,

        @Size(max = 500, message = "Comments must be at most 500 characters")
        String comments) {
}