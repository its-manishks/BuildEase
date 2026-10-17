package com.examly.springapp.dto.chat;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A chat message. There is deliberately no user id or role here: the backend takes the caller's
 * identity from the JWT only. history holds at most 6 earlier turns from the browser (never stored).
 */
public record ChatRequest(
        @NotBlank(message = "Please type a question")
        @Size(max = 500, message = "Please keep your question under 500 characters")
        String message,

        @Valid
        @Size(max = 6, message = "Too many previous messages")
        List<ChatTurn> history) {
}