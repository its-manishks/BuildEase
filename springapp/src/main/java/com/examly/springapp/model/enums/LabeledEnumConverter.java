package com.examly.springapp.model.enums;

import jakarta.persistence.AttributeConverter;

/**
 * Stores a LabeledEnum as its label ("Pending", "In Stock", "kg") - exactly the strings Phase 1
 * already saved - and reads either the label or the constant name back.
 * Subclass it once per enum with @Converter(autoApply = true) (see LabeledEnum).
 */
public abstract class LabeledEnumConverter<E extends Enum<E> & LabeledEnum> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected LabeledEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.getLabel();
    }

    @Override
    public E convertToEntityAttribute(String databaseValue) {
        return LabeledEnum.parse(type, databaseValue);
    }
}