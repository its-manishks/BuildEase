package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** How soon the customer needs a request fulfilled. */
public enum UrgencyLevel implements LabeledEnum {

    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String label;

    UrgencyLevel(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static UrgencyLevel fromValue(String value) {
        return LabeledEnum.parse(UrgencyLevel.class, value);
    }
}