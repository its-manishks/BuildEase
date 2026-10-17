package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Account role. Only the server assigns it; it is never accepted from signup. */
public enum UserRole implements LabeledEnum {

    ADMIN("Admin"),
    USER("User");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static UserRole fromValue(String value) {
        return LabeledEnum.parse(UserRole.class, value);
    }
}