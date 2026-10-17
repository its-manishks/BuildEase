package com.examly.springapp.config;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads "Authorization: Bearer <token>", validates it and puts the user into the security context.
 *
 * Phase 2 rules (enforced here, on the server, not only by the Angular popup):
 *  - disabled or unverified accounts are never authenticated
 *  - an account that must change its password (the seeded admin's first login) may call ONLY
 *    POST /api/auth/password/change and GET /api/users/me; every other API answers 403
 *    PASSWORD_CHANGE_REQUIRED
 * The user is reloaded from the database on every request, so the moment the password is
 * changed the same token works normally again.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            if (jwtUtils.validateToken(token)) {
                UserPrinciple principal = authenticate(token, request);
                if (principal != null && principal.isPasswordChangeRequired() && !isAllowedDuringPasswordChange(request)) {
                    writePasswordChangeRequired(request, response);
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private UserPrinciple authenticate(String token, HttpServletRequest request) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(jwtUtils.extractEmail(token));
            if (!userDetails.isEnabled() || !(userDetails instanceof UserPrinciple principal)) {
                return null;
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return principal;
        } catch (UsernameNotFoundException ex) {
            // The account behind the token no longer exists: stay unauthenticated.
            SecurityContextHolder.clearContext();
            return null;
        }
    }

    private boolean isAllowedDuringPasswordChange(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return ("POST".equals(method) && path.endsWith("/api/auth/password/change"))
                || ("GET".equals(method) && path.endsWith("/api/users/me"))
                || "OPTIONS".equals(method);
    }

    private void writePasswordChangeRequired(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":403,"
                + "\"error\":\"Forbidden\",\"errorCode\":\"PASSWORD_CHANGE_REQUIRED\","
                + "\"message\":\"Please change your password before continuing.\","
                + "\"path\":\"" + request.getRequestURI().replace("\"", "") + "\"}");
    }
}