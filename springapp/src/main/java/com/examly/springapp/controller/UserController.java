package com.examly.springapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.config.SecurityUtils;
import com.examly.springapp.dto.auth.UpdateProfileRequest;
import com.examly.springapp.dto.auth.UserResponse;
import com.examly.springapp.service.AccountService;

import jakarta.validation.Valid;

/** The signed-in account's own profile. The id always comes from the token, never from the URL. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AccountService accountService;

    public UserController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Returns the signed-in account's profile (used to prefill delivery details when ordering).
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(accountService.getProfile(SecurityUtils.getCurrentUserId()));
    }

    // Updates the signed-in account's saved delivery address.
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(accountService.updateProfile(SecurityUtils.getCurrentUserId(), request));
    }
}