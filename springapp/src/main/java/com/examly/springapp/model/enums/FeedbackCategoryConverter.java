package com.examly.springapp.model.enums;

import jakarta.persistence.Converter;

/** Stores FeedbackCategory as its label - the same strings Phase 1 saved (see LabeledEnumConverter). */
@Converter(autoApply = true)
public class FeedbackCategoryConverter extends LabeledEnumConverter<FeedbackCategory> {

    public FeedbackCategoryConverter() {
        super(FeedbackCategory.class);
    }
}