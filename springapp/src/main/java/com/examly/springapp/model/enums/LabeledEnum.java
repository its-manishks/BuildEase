package com.examly.springapp.model.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Contract for every Phase 2 enum that has a human-readable label (each module owns its enums).
 *   JSON / UI : the label, e.g. "Pending", "In Stock", "Cement", "kg"
 *   Database  : the SAME label, via a LabeledEnumConverter subclass, so rows written in
 *               Phase 1 ("Pending", "In Stock", ...) stay valid with no migration
 *   Input     : label or constant name, case-insensitive ("Pending", "pending", "PENDING")
 *
 * How to add one (example for RequestStatus):
 *   public enum RequestStatus implements LabeledEnum {
 *       PENDING("Pending"), ...;
 *       @JsonValue public String getLabel() { return label; }
 *       @JsonCreator public static RequestStatus fromValue(String v) { return LabeledEnum.parse(RequestStatus.class, v); }
 *   }
 *   @Converter(autoApply = true)
 *   public class RequestStatusConverter extends LabeledEnumConverter<RequestStatus> {
 *       public RequestStatusConverter() { super(RequestStatus.class); }
 *   }
 * Query parameters (?status=Pending) are converted automatically by EnumConversionConfig.
 */
public interface LabeledEnum {

    String getLabel();

    /** Parses a label or constant name; unknown values give a clear message listing the allowed ones. */
    static <E extends Enum<E> & LabeledEnum> E parse(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (E constant : type.getEnumConstants()) {
            if (constant.getLabel().equalsIgnoreCase(trimmed) || constant.name().equalsIgnoreCase(trimmed)) {
                return constant;
            }
        }
        String allowed = Arrays.stream(type.getEnumConstants())
                .map(LabeledEnum::getLabel)
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException("'" + trimmed + "' is not a valid " + type.getSimpleName()
                + ". Allowed values: " + allowed);
    }
}