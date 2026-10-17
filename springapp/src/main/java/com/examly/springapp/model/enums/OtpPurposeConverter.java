package com.examly.springapp.model.enums;

import jakarta.persistence.Converter;

/** Stores OtpPurpose as its label (see LabeledEnumConverter). */
@Converter(autoApply = true)
public class OtpPurposeConverter extends LabeledEnumConverter<OtpPurpose> {

    public OtpPurposeConverter() {
        super(OtpPurpose.class);
    }
}