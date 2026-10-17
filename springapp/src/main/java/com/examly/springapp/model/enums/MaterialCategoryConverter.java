package com.examly.springapp.model.enums;

import jakarta.persistence.Converter;

/** Stores MaterialCategory as its label - the same strings Phase 1 saved (see LabeledEnumConverter). */
@Converter(autoApply = true)
public class MaterialCategoryConverter extends LabeledEnumConverter<MaterialCategory> {

    public MaterialCategoryConverter() {
        super(MaterialCategory.class);
    }
}