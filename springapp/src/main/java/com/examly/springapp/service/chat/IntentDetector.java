package com.examly.springapp.service.chat;

import java.util.List;
import java.util.Locale;

/**
 * Rule-based intent detection. This decides which data may be fetched - not the AI model.
 * Requests for secrets or other people's data are classified SENSITIVE and answered with a refusal
 * before any lookup or AI call happens.
 */
public final class IntentDetector {

    private static final List<String> SENSITIVE_HINTS = List.of(
            "password", "otp", "token", "api key", "apikey", "secret", "credential",
            "all users", "other users", "another user", "other customers", "all customers", "every user",
            "user list", "list of users", "email of", "emails of", "phone number of", "mobile number of",
            "address of", "their orders", "his orders", "her orders", "someone else",
            "ignore previous", "ignore your", "ignore all", "disregard", "system prompt", "your instructions",
            "database", "sql", "dump");

    private IntentDetector() {
        // Utility class
    }

    /** Intent for the given role ("Admin", "User" or null for visitors). */
    public static ChatIntent detect(String message, String role) {
        String text = " " + message.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ") + " ";
        if (containsAny(text, SENSITIVE_HINTS)) {
            return ChatIntent.SENSITIVE;
        }
        if ("Admin".equals(role)) {
            return adminIntent(text);
        }
        return customerIntent(text);
    }

    private static ChatIntent adminIntent(String text) {
        if (containsAny(text, List.of("out of stock", "sold out", "no stock", "zero stock", "unavailable"))) {
            return ChatIntent.ADMIN_OUT_OF_STOCK;
        }
        if (containsAny(text, List.of("pending", "awaiting approval", "need approval", "to approve", "approval"))) {
            return ChatIntent.ADMIN_PENDING;
        }
        if (containsAny(text, List.of("shipped", "delivered", "dispatched"))) {
            return ChatIntent.ADMIN_SHIPPED;
        }
        if (containsAny(text, List.of("how many orders", "how many requests", "total orders", "total requests",
                "orders placed", "requests placed", "number of orders", "number of requests"))) {
            return ChatIntent.ADMIN_ORDER_TOTALS;
        }
        if (containsAny(text, List.of("stock", "inventory", "how many materials", "materials available",
                "available materials", "low stock", "running low"))) {
            return ChatIntent.ADMIN_STOCK;
        }
        return ChatIntent.GENERAL;
    }

    private static ChatIntent customerIntent(String text) {
        boolean personal = containsAny(text, List.of(" my ", " i ", " me ", " mine ", " i've ", " ive "));
        if (!personal) {
            return ChatIntent.GENERAL;
        }
        if (containsAny(text, List.of("recommend", "suggest", "relevant", "should i buy", "similar", "what else"))
                && !containsAny(text, List.of("how do i", "how can i"))) {
            return ChatIntent.MY_RECOMMENDATIONS;
        }
        if (containsAny(text, List.of("review", "feedback", "rate "))
                && !containsAny(text, List.of("how do i write", "how can i write", "how to write"))) {
            return ChatIntent.MY_REVIEWS;
        }
        if (containsAny(text, List.of("latest", "last order", "last request", "recent order", "recent request",
                "status"))) {
            return ChatIntent.MY_LATEST_ORDER;
        }
        if (containsAny(text, List.of("which material", "what material", "materials have i", "ordered before",
                "ordered previously", "previously ordered", "what did i order", "what have i ordered", "quantity"))) {
            return ChatIntent.MY_MATERIALS;
        }
        if (containsAny(text, List.of(" orders", " order ", "requests", " request ", "purchase", "bought"))
                && !containsAny(text, List.of("how do i", "how can i", "how to"))) {
            return ChatIntent.MY_ORDERS;
        }
        return ChatIntent.GENERAL;
    }

    private static boolean containsAny(String text, List<String> needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}