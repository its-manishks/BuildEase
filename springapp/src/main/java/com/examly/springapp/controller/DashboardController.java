package com.examly.springapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.config.SecurityUtils;
import com.examly.springapp.dto.dashboard.AdminDashboardResponse;
import com.examly.springapp.dto.dashboard.UserDashboardResponse;
import com.examly.springapp.service.DashboardService;

/** Dashboards. /admin is Admin-only and /me is User-only (SecurityConfig). */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Returns live business metrics and chart data for the admin dashboard.
    @GetMapping("/admin")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    // Returns the signed-in user's own request and review summary for the Home page.
    @GetMapping("/me")
    public ResponseEntity<UserDashboardResponse> getMyDashboard() {
        return ResponseEntity.ok(dashboardService.getUserDashboard(SecurityUtils.getCurrentUserId()));
    }
}