package com.examly.springapp.service;

import com.examly.springapp.dto.dashboard.AdminDashboardResponse;
import com.examly.springapp.dto.dashboard.UserDashboardResponse;

/** Dashboard figures, always computed from live data. */
public interface DashboardService {

    AdminDashboardResponse getAdminDashboard();

    UserDashboardResponse getUserDashboard(Long userId);
}