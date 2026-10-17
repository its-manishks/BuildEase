package com.examly.springapp.service.ai;

/**
 * The AI service is switched off (no GEMINI_API_KEY) or failed (timeout, quota, error, empty answer).
 * Callers catch it and fall back, so the rest of BuildEase keeps working without AI.
 */
public class AiUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public AiUnavailableException(String message) {
        super(message);
    }
}