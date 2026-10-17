package com.examly.springapp.config;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.examly.springapp.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Creates and validates JSON Web Tokens (jjwt 0.9.1, HS512).
 *
 * The secret comes from the JWT_SECRET environment variable (jwt.secret); nothing secret is
 * committed. If it is not set, a random key is generated at startup: the app still works, but
 * every restart logs everyone out. Set JWT_SECRET in any shared or real environment.
 */
@Component
public class JwtUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class);
    private static final int GENERATED_KEY_BYTES = 64;

    private final byte[] signingKey;
    private final long expirationMs;

    public JwtUtils(@Value("${jwt.secret:}") String secret,
                    @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.signingKey = resolveKey(secret);
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getUserId())
                .claim("role", user.getUserRole())
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(SignatureAlgorithm.HS512, signingKey)
                .compact();
    }

    /** Returns the email stored as the token subject. */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /** True when the signature is valid and the token has not expired. */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(signingKey)
                .parseClaimsJws(token)
                .getBody();
    }

    private static byte[] resolveKey(String secret) {
        if (secret != null && !secret.isBlank()) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
        LOGGER.warn("JWT_SECRET is not set - using a random key; all tokens become invalid after a restart.");
        byte[] generated = new byte[GENERATED_KEY_BYTES];
        new SecureRandom().nextBytes(generated);
        return generated;
    }
}