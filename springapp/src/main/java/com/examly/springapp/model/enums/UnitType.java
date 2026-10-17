package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Allowed units for pricing and stock (same values as the Angular dropdown). */
public enum UnitType implements LabeledEnum {

    KG("kg"),
    TON("ton"),
    PIECE("piece"),
    BAG("bag"),
    LITRE("litre"),
    METER("meter"),
    SQ_FT("sq.ft");

    private final String label;

    UnitType(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static UnitType fromValue(String value) {
        return LabeledEnum.parse(UnitType.class, value);
    }
}