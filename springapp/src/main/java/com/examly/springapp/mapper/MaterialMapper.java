package com.examly.springapp.mapper;

import java.time.ZoneOffset;

import com.examly.springapp.dto.material.MaterialResponse;
import com.examly.springapp.model.Material;

/** Material entity → API response. */
public final class MaterialMapper {

    /** At or below this many units (but above zero) a material counts as "low stock". */
    public static final int LOW_STOCK_THRESHOLD = 10;

    private MaterialMapper() {
        // Utility class
    }

    public static MaterialResponse toResponse(Material material) {
        int stock = material.stockOnHand();
        String imageUrl = material.getImageUpdatedAt() == null ? null
                : "/api/material/" + material.getMaterialId() + "/image?v="
                  + material.getImageUpdatedAt().toEpochSecond(ZoneOffset.UTC);
        return new MaterialResponse(
                material.getMaterialId(),
                material.getMaterialName(),
                material.getDescription(),
                material.getCategory(),
                material.getPricePerUnit(),
                material.getUnitType(),
                stock,
                material.getAvailabilityStatus(),
                stock > 0 && stock <= LOW_STOCK_THRESHOLD,
                !material.isArchived(),
                imageUrl,
                material.getVersion());
    }
}