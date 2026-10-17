package com.examly.springapp.model.enums;

import jakarta.persistence.Converter;

/** Stores UnitType as its label - the same strings Phase 1 saved (see LabeledEnumConverter). */
@Converter(autoApply = true)
public class UnitTypeConverter extends LabeledEnumConverter<UnitType> {

    public UnitTypeConverter() {
        super(UnitType.class);
    }
}