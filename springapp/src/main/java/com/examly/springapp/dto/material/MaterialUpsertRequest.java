package com.examly.springapp.dto.material;

import com.examly.springapp.model.enums.MaterialCategory;
import com.examly.springapp.model.enums.UnitType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create or edit a material. Category and unit must be one of the allowed values (enum),
 * so a forged value from outside the dropdown is rejected with 400.
 *  - quantity: initial stock, required on create, ignored on edit (use PATCH /stock)
 *  - version : required on edit (optimistic locking: rejects stale edits)
 *  - active  : optional on edit; true restores an archived material
 */
public record MaterialUpsertRequest(
        @NotBlank(message = "Material name is required")
        @Size(max = 100, message = "Material name must be at most 100 characters")
        String materialName,

        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotNull(message = "Category is required")
        MaterialCategory category,

        @NotNull(message = "Price per unit is required")
        @DecimalMin(value = "0.01", message = "Price per unit must be greater than 0")
        @DecimalMax(value = "10000000", message = "Price per unit must be at most 1,00,00,000")
        Double pricePerUnit,

        @NotNull(message = "Unit type is required")
        UnitType unitType,

        @Min(value = 0, message = "Quantity cannot be negative")
        @Max(value = 1000000, message = "Quantity must be at most 10,00,000")
        Integer quantity,

        Long version,

        Boolean active) {
}