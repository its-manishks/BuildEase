package com.examly.springapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.config.SecurityUtils;
import com.examly.springapp.dto.admin.AdminStatusRequest;
import com.examly.springapp.dto.admin.CreateAdminRequest;
import com.examly.springapp.dto.auth.UserResponse;
import com.examly.springapp.dto.common.PageResponse;
import com.examly.springapp.service.AdminManagementService;

import jakarta.validation.Valid;

/**
 * Admin accounts. SecurityConfig allows only Admins here; the service additionally requires the
 * PRIMARY admin for every change.
 */
@RestController
@RequestMapping("/api/admin/admins")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    public AdminManagementController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    // Lists admin accounts, one page at a time (any admin may view the list).
    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> listAdmins(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminManagementService.listAdmins(page, size));
    }

    // Creates a new admin with a temporary password (primary admin only).
    @PostMapping
    public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminManagementService.createAdmin(SecurityUtils.getCurrentUserId(), request));
    }

    // Disables or re-enables an admin account (primary admin only; the primary admin itself is protected).
    @PatchMapping("/{adminId}")
    public ResponseEntity<UserResponse> setAdminActive(@PathVariable("adminId") Long adminId,
                                                       @Valid @RequestBody AdminStatusRequest request) {
        return ResponseEntity.ok(adminManagementService.setAdminActive(SecurityUtils.getCurrentUserId(), adminId,
                request.active()));
    }
}