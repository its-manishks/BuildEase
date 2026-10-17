package com.examly.springapp.model.enums;

import jakarta.persistence.Converter;

/** Stores TimeSlot as its label - the same strings Phase 1 saved (see LabeledEnumConverter). */
@Converter(autoApply = true)
public class TimeSlotConverter extends LabeledEnumConverter<TimeSlot> {

    public TimeSlotConverter() {
        super(TimeSlot.class);
    }
}