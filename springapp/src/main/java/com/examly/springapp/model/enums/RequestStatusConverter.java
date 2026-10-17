package com.examly.springapp.model.enums;

import jakarta.persistence.Converter;

/** Stores RequestStatus as its label - the same strings Phase 1 saved (see LabeledEnumConverter). */
@Converter(autoApply = true)
public class RequestStatusConverter extends LabeledEnumConverter<RequestStatus> {

    public RequestStatusConverter() {
        super(RequestStatus.class);
    }
}