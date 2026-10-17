package com.examly.springapp.dto.material;

import com.examly.springapp.model.enums.MaterialCategory;
import com.examly.springapp.model.enums.UnitType;

/**
 * What the API returns for a material. imageUrl is relative to the API base URL and changes
 * whenever a new image is uploaded (so browsers never show a stale cached picture).
 */
public record MaterialResponse(
        Long materialId,
        String materialName,
        String description,
        MaterialCategory category,
        Double pricePerUnit,
        UnitType unitType,
        int quantity,
        String availabilityStatus,
        boolean lowStock,
        boolean active,
        String imageUrl,
        Long version) {
}