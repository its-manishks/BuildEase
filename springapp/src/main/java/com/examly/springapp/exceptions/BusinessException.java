package com.examly.springapp.exceptions;

/**
 * Base class for every expected business-rule failure in Phase 2.
 * Throw it with a code and a user-friendly message:
 *   throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "Only 5 bags of Cement are left");
 * GlobalExceptionHandler turns it into the code's HTTP status and the standard error body.
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}