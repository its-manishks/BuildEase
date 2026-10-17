package com.examly.springapp.service;

import com.examly.springapp.dto.admin.CreateAdminRequest;
import com.examly.springapp.dto.auth.UserResponse;
import com.examly.springapp.dto.common.PageResponse;

/** Admin accounts. Only the primary (seeded) admin may create, disable or enable admins. */
public interface AdminManagementService {

    PageResponse<UserResponse> listAdmins(int page, int size);

    UserResponse createAdmin(Long actingUserId, CreateAdminRequest request);

    UserResponse setAdminActive(Long actingUserId, Long adminId, boolean active);
}