package com.examly.springapp.mapper;

import com.examly.springapp.dto.auth.UserResponse;
import com.examly.springapp.model.User;
import com.examly.springapp.model.enums.AccountStatus;

/** Entity → safe response. Legacy (Phase 1) accounts with empty flags count as verified and active. */
public final class UserMapper {

    private UserMapper() {
        // Utility class
    }

    public static UserResponse toResponse(User user) {
        AccountStatus status = user.getAccountStatus() == null ? AccountStatus.ACTIVE : user.getAccountStatus();
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getAddress(),
                user.getUserRole(),
                user.isPrimaryAdminAccount(),
                !Boolean.FALSE.equals(user.getEmailVerified()),
                !Boolean.FALSE.equals(user.getMobileVerified()),
                status.getLabel(),
                user.getCreatedAt());
    }
}