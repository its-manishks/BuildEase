package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Request lifecycle: Pending -> Approved | Rejected, Approved -> Shipped. Shipped unlocks reviews. */
public enum RequestStatus implements LabeledEnum {

    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    SHIPPED("Shipped");

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static RequestStatus fromValue(String value) {
        return LabeledEnum.parse(RequestStatus.class, value);
    }
}