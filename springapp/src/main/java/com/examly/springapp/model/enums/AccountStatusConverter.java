package com.examly.springapp.model.enums;

import jakarta.persistence.Converter;

/** Stores AccountStatus as its label (see LabeledEnumConverter). */
@Converter(autoApply = true)
public class AccountStatusConverter extends LabeledEnumConverter<AccountStatus> {

    public AccountStatusConverter() {
        super(AccountStatus.class);
    }
}