package com.examly.springapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Role rules for EVERY Phase 2 endpoint, written once in Step 1 so no other member edits this file.
 * Endpoints a later step has not built yet simply answer 404 until that step is merged.
 *
 * Rules are evaluated top to bottom: specific paths sit above general ones.
 * Ownership ("only your own request") and "primary admin only" are checked in the services.
 * OWNERSHIP: only Manish edits this file.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "Admin";
    private static final String USER = "User";

    private final JwtUtils jwtUtils;
    private final MyUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtUtils jwtUtils,
                          MyUserDetailsService userDetailsService,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                    // ---------- Public ----------
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/register", "/api/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/otp/verify", "/api/auth/otp/resend").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/password/forgot", "/api/auth/password/reset").permitAll()
                    // Images are not sensitive and <img src> cannot send a token
                    .requestMatchers(HttpMethod.GET, "/api/material/*/image").permitAll()
                    // Final phase: the assistant is available on the login page; with a valid token the
                    // JWT filter identifies the caller and ChatService personalises (identity never from the body)
                    .requestMatchers(HttpMethod.POST, "/api/chat").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/ai/status").permitAll()

                    // ---------- Any logged-in account (Manish) ----------
                    .requestMatchers(HttpMethod.POST, "/api/auth/password/change").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()

                    // ---------- Admin management (Somaan; primary admin checked in the service) ----------
                    .requestMatchers("/api/admin/**").hasAuthority(ADMIN)

                    // ---------- Materials (Aakash) ----------
                    .requestMatchers(HttpMethod.GET, "/api/material", "/api/material/*").hasAnyAuthority(ADMIN, USER)
                    .requestMatchers(HttpMethod.POST, "/api/material", "/api/material/*/image").hasAuthority(ADMIN)
                    .requestMatchers(HttpMethod.PUT, "/api/material/*").hasAuthority(ADMIN)
                    .requestMatchers(HttpMethod.PATCH, "/api/material/*/stock").hasAuthority(ADMIN)
                    .requestMatchers(HttpMethod.DELETE, "/api/material/*").hasAuthority(ADMIN)

                    // ---------- Material requests (Anuj: user side, Somaan: admin side) ----------
                    .requestMatchers(HttpMethod.GET, "/api/materialRequest/admin/insights").hasAuthority(ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/materialRequest/my/history").hasAuthority(USER)
                    .requestMatchers(HttpMethod.GET, "/api/materialRequest/user/*").hasAuthority(USER)
                    .requestMatchers(HttpMethod.GET, "/api/materialRequest").hasAuthority(ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/materialRequest/*").hasAnyAuthority(ADMIN, USER)
                    .requestMatchers(HttpMethod.POST, "/api/materialRequest").hasAuthority(USER)
                    .requestMatchers(HttpMethod.PATCH, "/api/materialRequest/*/status").hasAuthority(ADMIN)
                    .requestMatchers(HttpMethod.PATCH, "/api/materialRequest/*/quantity").hasAuthority(USER)
                    // Phase 1 endpoint, kept Admin-only until Somaan replaces it with the PATCH above
                    .requestMatchers(HttpMethod.PUT, "/api/materialRequest/*").hasAuthority(ADMIN)
                    .requestMatchers(HttpMethod.DELETE, "/api/materialRequest/*").hasAuthority(USER)

                    // ---------- Feedback / reviews (Jeevasri) ----------
                    .requestMatchers(HttpMethod.GET, "/api/feedback/user/*").hasAuthority(USER)
                    .requestMatchers(HttpMethod.GET, "/api/feedback").hasAuthority(ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/feedback/*").hasAnyAuthority(ADMIN, USER)
                    .requestMatchers(HttpMethod.POST, "/api/feedback/spellcheck", "/api/feedback/improve").hasAuthority(USER)
                    .requestMatchers(HttpMethod.POST, "/api/feedback").hasAuthority(USER)
                    .requestMatchers(HttpMethod.DELETE, "/api/feedback/*").hasAuthority(USER)

                    // ---------- Recommendations (final phase; own history from the token) ----------
                    .requestMatchers(HttpMethod.GET, "/api/recommendations", "/api/recommendations/**").hasAuthority(USER)

                    // ---------- Dashboard (Jeevasri) ----------
                    .requestMatchers(HttpMethod.GET, "/api/dashboard/admin").hasAuthority(ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/dashboard/me").hasAuthority(USER)

                    // Anything else needs a valid token
                    .anyRequest().authenticated())
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtils, userDetailsService),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}