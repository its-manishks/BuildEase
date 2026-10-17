package com.examly.springapp.dto.chat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** One earlier message of the conversation, kept only in the browser and sent back for context. */
public record ChatTurn(
        @Pattern(regexp = "user|model", message = "role must be 'user' or 'model'")
        String role,

        @Size(max = 2000, message = "A previous message is too long")
        String text) {
}