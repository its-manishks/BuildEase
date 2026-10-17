package com.examly.springapp.dto.chat;

/** Whether AI features (Gemini) are configured, so the UI can show or hide AI-only controls. */
public record AiStatusResponse(boolean enabled) {
}