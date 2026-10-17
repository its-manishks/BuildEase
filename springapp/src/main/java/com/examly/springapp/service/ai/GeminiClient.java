package com.examly.springapp.service.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.examly.springapp.dto.chat.ChatTurn;
import com.examly.springapp.logging.LogMasker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The only class that talks to Google's Gemini API (REST generateContent, JDK HttpClient - no SDK).
 *
 * Security:
 *  - the key comes from GEMINI_API_KEY and is sent in the x-goog-api-key header (never in the URL,
 *    so it cannot appear in access logs); it is never returned to the browser
 *  - prompts and replies are never logged; failures log only the HTTP status and Google's error status
 *  - when the key is missing or Gemini fails, AiUnavailableException lets callers fall back
 */
@Component
public class GeminiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiClient.class);
    private static final int MAX_REPLY_CHARS = 2000;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int timeoutSeconds;

    public GeminiClient(@Value("${app.ai.gemini.api-key:}") String apiKey,
                        @Value("${app.ai.gemini.model:gemini-flash-latest}") String model,
                        @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
                        @Value("${app.ai.gemini.timeout-seconds:15}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gemini-flash-latest" : model.trim();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.timeoutSeconds = timeoutSeconds;
    }

    /** True when a key is configured (it does not prove the key is valid). */
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /**
     * Asks Gemini for a text answer.
     * @param systemInstruction rules and background for the model (never contains personal data)
     * @param history           earlier turns of this conversation (may be empty)
     * @param userText          the new message, treated by the instructions as data, not as commands
     */
    public String generate(String systemInstruction, List<ChatTurn> history, String userText)
            throws AiUnavailableException {
        if (!isConfigured()) {
            throw new AiUnavailableException("GEMINI_API_KEY is not set");
        }
        String body = buildRequestBody(systemInstruction, history, userText);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/models/" + model + ":generateContent"))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            LOGGER.warn("Gemini unreachable: {}", LogMasker.sanitize(ex.getMessage()));
            throw new AiUnavailableException("Gemini unreachable");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Gemini call interrupted");
        }
        if (response.statusCode() / 100 != 2) {
            LOGGER.warn("Gemini refused the request: HTTP {} {}", response.statusCode(), errorStatus(response.body()));
            throw new AiUnavailableException("Gemini returned HTTP " + response.statusCode());
        }
        String text = extractText(response.body());
        if (text.isBlank()) {
            throw new AiUnavailableException("Gemini returned no text");
        }
        return text.length() > MAX_REPLY_CHARS ? text.substring(0, MAX_REPLY_CHARS) + "…" : text;
    }

    String buildRequestBody(String systemInstruction, List<ChatTurn> history, String userText) {
        ObjectNode root = objectMapper.createObjectNode();
        root.putObject("systemInstruction").putArray("parts").addObject().put("text", systemInstruction);
        ArrayNode contents = root.putArray("contents");
        if (history != null) {
            for (ChatTurn turn : history) {
                if (turn == null || turn.text() == null || turn.text().isBlank()) {
                    continue;
                }
                ObjectNode content = contents.addObject();
                content.put("role", "model".equals(turn.role()) ? "model" : "user");
                content.putArray("parts").addObject().put("text", turn.text());
            }
        }
        ObjectNode current = contents.addObject();
        current.put("role", "user");
        current.putArray("parts").addObject().put("text", userText);
        ObjectNode config = root.putObject("generationConfig");
        config.put("temperature", 0.3);
        config.put("maxOutputTokens", 2048);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not build the Gemini request", ex);
        }
    }

    /** Joins the text parts of the first candidate; empty when blocked or missing. */
    String extractText(String json) {
        try {
            JsonNode parts = objectMapper.readTree(json).path("candidates").path(0).path("content").path("parts");
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                text.append(parts.path(i).path("text").asText(""));
            }
            return text.toString().trim();
        } catch (IOException ex) {
            return "";
        }
    }

    private String errorStatus(String json) {
        try {
            return objectMapper.readTree(json).path("error").path("status").asText("-");
        } catch (IOException ex) {
            return "-";
        }
    }
}