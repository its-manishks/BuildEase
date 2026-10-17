package com.examly.springapp.dto.request;

import java.time.LocalDate;

import com.examly.springapp.model.enums.MaterialCategory;
import com.examly.springapp.model.enums.RequestStatus;
import com.examly.springapp.model.enums.UnitType;

/**
 * One card in the "previous orders" carousel: the latest approved/shipped order of a material, plus
 * whether it can be ordered again right now (and, if not, why).
 */
public record OrderHistoryItem(
        Long materialRequestId,
        Long materialId,
        String materialName,
        MaterialCategory category,
        UnitType unitType,
        Double pricePerUnit,
        String imageUrl,
        int lastQuantity,
        RequestStatus status,
        LocalDate requestDate,
        int availableQuantity,
        boolean canReorder,
        String reorderBlockedReason) {
}