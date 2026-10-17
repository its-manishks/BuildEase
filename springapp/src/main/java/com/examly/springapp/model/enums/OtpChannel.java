package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Where a one-time password is delivered. */
public enum OtpChannel implements LabeledEnum {

    EMAIL("Email"),
    SMS("SMS");

    private final String label;

    OtpChannel(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static OtpChannel fromValue(String value) {
        return LabeledEnum.parse(OtpChannel.class, value);
    }
}