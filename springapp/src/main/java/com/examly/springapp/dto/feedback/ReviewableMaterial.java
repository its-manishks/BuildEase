package com.examly.springapp.dto.feedback;

import com.examly.springapp.model.enums.MaterialCategory;

/** A material the user received and can review now. */
public record ReviewableMaterial(Long materialId, String materialName, MaterialCategory category, String imageUrl) {
}