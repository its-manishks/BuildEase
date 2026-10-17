package com.examly.springapp.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Lifecycle of an account: created unverified, active after email and mobile OTP, or disabled. */
public enum AccountStatus implements LabeledEnum {

    PENDING_VERIFICATION("Pending Verification"),
    ACTIVE("Active"),
    DISABLED("Disabled");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static AccountStatus fromValue(String value) {
        return LabeledEnum.parse(AccountStatus.class, value);
    }
}