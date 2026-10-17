package com.examly.springapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.config.SecurityUtils;
import com.examly.springapp.dto.auth.AuthResponse;
import com.examly.springapp.dto.auth.ChangePasswordRequest;
import com.examly.springapp.dto.auth.RegisterRequest;
import com.examly.springapp.dto.auth.RegisterResponse;
import com.examly.springapp.dto.auth.ResendOtpRequest;
import com.examly.springapp.dto.auth.VerificationStatusResponse;
import com.examly.springapp.dto.auth.VerifyOtpRequest;
import com.examly.springapp.dto.common.MessageResponse;
import com.examly.springapp.model.LoginDTO;
import com.examly.springapp.service.UserService;

import jakarta.validation.Valid;

/**
 * Signup, verification, login and password-change APIs. Only DTOs cross this boundary - never entities.
 * Forgot/reset password lives in PasswordResetController (Step 3).
 * Errors are thrown by the service and answered by GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Creates an unverified User account (role is always User) and sends email + SMS verification codes.
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    // Logs in with a username or email; returns the token and whether a password change is mandatory.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(userService.login(loginDTO.getIdentifier(), loginDTO.getPassword()));
    }

    // Checks a signup code for one channel; the account becomes Active once email and mobile are both verified.
    @PostMapping("/auth/otp/verify")
    public ResponseEntity<VerificationStatusResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(userService.verifySignupCode(request.email(), request.channel(), request.code()));
    }

    // Sends a fresh signup code on one channel, respecting the cooldown and hourly limit.
    @PostMapping("/auth/otp/resend")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(userService.resendSignupCode(request.email(), request.channel()));
    }

    // Changes the logged-in account's password (mandatory on the seeded admin's first login) and returns a fresh session.
    @PostMapping("/auth/password/change")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(SecurityUtils.getCurrentUserId(), request));
    }
}