package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Allowed material categories (same values as the Angular dropdown). */
public enum MaterialCategory implements LabeledEnum {

    CEMENT("Cement"),
    METAL("Metal"),
    WOOD("Wood"),
    TILES("Tiles"),
    ELECTRICAL("Electrical"),
    PLUMBING("Plumbing"),
    PAINT("Paint"),
    BRICKS("Bricks"),
    SAND("Sand"),
    HARDWARE("Hardware");

    private final String label;

    MaterialCategory(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static MaterialCategory fromValue(String value) {
        return LabeledEnum.parse(MaterialCategory.class, value);
    }
}