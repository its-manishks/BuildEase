package com.examly.springapp.dto.chat;

/**
 * The assistant's reply.
 * source: "ai" (written by Gemini) or "assistant" (written by BuildEase itself from your own data or
 * the built-in help). loginRequired tells the widget to suggest logging in.
 */
public record ChatResponse(String reply, String source, boolean loginRequired) {

    public static ChatResponse assistant(String reply) {
        return new ChatResponse(reply, "assistant", false);
    }

    public static ChatResponse ai(String reply) {
        return new ChatResponse(reply, "ai", false);
    }

    public static ChatResponse loginRequired(String reply) {
        return new ChatResponse(reply, "assistant", true);
    }
}