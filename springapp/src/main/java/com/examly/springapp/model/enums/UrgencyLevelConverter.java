package com.examly.springapp.model.enums;

import jakarta.persistence.Converter;

/** Stores UrgencyLevel as its label - the same strings Phase 1 saved (see LabeledEnumConverter). */
@Converter(autoApply = true)
public class UrgencyLevelConverter extends LabeledEnumConverter<UrgencyLevel> {

    public UrgencyLevelConverter() {
        super(UrgencyLevel.class);
    }
}