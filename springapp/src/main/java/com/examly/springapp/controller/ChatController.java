package com.examly.springapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.dto.chat.AiStatusResponse;
import com.examly.springapp.dto.chat.ChatRequest;
import com.examly.springapp.dto.chat.ChatResponse;
import com.examly.springapp.service.ai.GeminiClient;
import com.examly.springapp.service.chat.ChatService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * BuildEase assistant API. Public (it is available on the login page); when a valid token is sent,
 * answers are personalised for that account. Identity always comes from the token, never from the body.
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final GeminiClient gemini;

    public ChatController(ChatService chatService, GeminiClient gemini) {
        this.chatService = chatService;
        this.gemini = gemini;
    }

    // Answers one chat message (general help for visitors; own-data answers for signed-in users).
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(chatService.reply(request, clientIp(httpRequest)));
    }

    // Tells the frontend whether AI features are configured (never exposes the key).
    @GetMapping("/ai/status")
    public ResponseEntity<AiStatusResponse> status() {
        return ResponseEntity.ok(new AiStatusResponse(gemini.isConfigured()));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}