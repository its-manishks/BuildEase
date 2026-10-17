package com.examly.springapp.exceptions;

/**
 * Thrown when a material cannot be deleted because requests or feedback still reference it. Mapped to 409 Conflict.
 */
public class MaterialDeletionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MaterialDeletionException(String message) {
        super(message);
    }
}