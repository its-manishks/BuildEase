package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Preferred delivery time slot. */
public enum TimeSlot implements LabeledEnum {

    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening");

    private final String label;

    TimeSlot(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static TimeSlot fromValue(String value) {
        return LabeledEnum.parse(TimeSlot.class, value);
    }
}