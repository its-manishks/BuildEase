package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Allowed review categories. */
public enum FeedbackCategory implements LabeledEnum {

    SERVICE("Service"),
    PRICING("Pricing"),
    QUALITY("Quality"),
    DELIVERY("Delivery"),
    OTHER("Other");

    private final String label;

    FeedbackCategory(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static FeedbackCategory fromValue(String value) {
        return LabeledEnum.parse(FeedbackCategory.class, value);
    }
}