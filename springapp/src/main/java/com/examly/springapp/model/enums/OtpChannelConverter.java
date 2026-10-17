package com.examly.springapp.model.enums;

import jakarta.persistence.Converter;

/** Stores OtpChannel as its label (see LabeledEnumConverter). */
@Converter(autoApply = true)
public class OtpChannelConverter extends LabeledEnumConverter<OtpChannel> {

    public OtpChannelConverter() {
        super(OtpChannel.class);
    }
}