package com.examly.springapp.exceptions;

/**
 * Thrown when a material request breaks a business rule (duplicate request, out-of-stock material, invalid dates or status). Mapped to 409 Conflict.
 */
public class MaterialRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MaterialRequestException(String message) {
        super(message);
    }
}