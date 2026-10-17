package com.examly.springapp.service;

import com.examly.springapp.dto.auth.UpdateProfileRequest;
import com.examly.springapp.dto.auth.UserResponse;
import com.examly.springapp.dto.common.MessageResponse;

/**
 * Self-service account features (Step 3, Aakash): the signed-in user's profile and the
 * forgot/reset password flow. Signup, login and password change are in UserService (Step 2).
 */
public interface AccountService {

    UserResponse getProfile(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    /** Emails a reset code if the account exists; the answer is the same either way. */
    MessageResponse requestPasswordReset(String email);

    /** Sets a new password using the emailed reset code. */
    MessageResponse resetPassword(String email, String code, String newPassword);
}