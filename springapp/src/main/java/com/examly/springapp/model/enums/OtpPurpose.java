package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Why a one-time password was issued. A code for one purpose never works for another. */
public enum OtpPurpose implements LabeledEnum {

    SIGNUP_VERIFICATION("Signup Verification"),
    PASSWORD_RESET("Password Reset");

    private final String label;

    OtpPurpose(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static OtpPurpose fromValue(String value) {
        return LabeledEnum.parse(OtpPurpose.class, value);
    }
}