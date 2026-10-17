package com.examly.springapp.service.chat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sliding-window rate limit for AI features (protects the Gemini quota and stops abuse of the
 * public chat endpoint). Keys: "ip:..." for visitors, "user:..." for signed-in accounts.
 * In memory: limits reset on restart, which is acceptable for this purpose.
 */
@Component
public class ChatRateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();
    private final int anonymousLimit;
    private final int userLimit;
    private final long windowMillis;

    public ChatRateLimiter(@Value("${app.ai.chat.anonymous-limit:15}") int anonymousLimit,
                           @Value("${app.ai.chat.user-limit:40}") int userLimit,
                           @Value("${app.ai.chat.window-minutes:5}") int windowMinutes) {
        this.anonymousLimit = anonymousLimit;
        this.userLimit = userLimit;
        this.windowMillis = windowMinutes * 60_000L;
    }

    /** Records one call; false when the caller has used up their allowance for the current window. */
    public boolean tryAcquire(String key, boolean signedIn) {
        long now = System.currentTimeMillis();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        }
        Deque<Long> calls = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (calls) {
            while (!calls.isEmpty() && now - calls.peekFirst() > windowMillis) {
                calls.pollFirst();
            }
            if (calls.size() >= (signedIn ? userLimit : anonymousLimit)) {
                return false;
            }
            calls.addLast(now);
            return true;
        }
    }

    private boolean isExpired(Deque<Long> calls, long now) {
        synchronized (calls) {
            return calls.isEmpty() || now - calls.peekLast() > windowMillis;
        }
    }
}