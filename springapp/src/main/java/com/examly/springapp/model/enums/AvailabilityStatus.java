package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Derived from stock quantity: never set by hand in Phase 2. */
public enum AvailabilityStatus implements LabeledEnum {

    IN_STOCK("In Stock"),
    OUT_OF_STOCK("Out of Stock");

    private final String label;

    AvailabilityStatus(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static AvailabilityStatus fromValue(String value) {
        return LabeledEnum.parse(AvailabilityStatus.class, value);
    }
}