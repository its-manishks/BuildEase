package com.examly.springapp.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.examly.springapp.dto.admin.CreateAdminRequest;
import com.examly.springapp.dto.auth.UserResponse;
import com.examly.springapp.dto.common.PageResponse;
import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.logging.Auditable;
import com.examly.springapp.mapper.UserMapper;
import com.examly.springapp.model.User;
import com.examly.springapp.model.enums.AccountStatus;
import com.examly.springapp.model.enums.UserRole;
import com.examly.springapp.repository.UserRepo;

/**
 * Admin management rules:
 *  - only the primary admin may create, disable or enable admins (checked here, from the database,
 *    not from anything the client sends)
 *  - the primary admin can never be disabled
 *  - new admins are verified, active and MUST change their temporary password at first login
 *  - there is no API that turns a User into an Admin, so nobody can promote themselves
 */
@Service
public class AdminManagementServiceImpl implements AdminManagementService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminManagementServiceImpl(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResponse<UserResponse> listAdmins(int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("userId").ascending());
        return PageResponse.map(userRepo.findByUserRole(UserRole.ADMIN.getLabel(), pageRequest), UserMapper::toResponse);
    }

    @Override
    @Auditable(action = "ADMIN_CREATED", resource = "User")
    public UserResponse createAdmin(Long actingUserId, CreateAdminRequest request) {
        requirePrimaryAdmin(actingUserId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = request.username().trim().replaceAll("\\s+", " ");
        String mobile = request.mobileNumber().trim();
        rejectDuplicates(email, username, mobile);

        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setMobileNumber(mobile);
        admin.setPassword(passwordEncoder.encode(request.temporaryPassword()));
        admin.setUserRole(UserRole.ADMIN.getLabel());
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setEmailVerified(true);
        admin.setMobileVerified(true);
        admin.setMustChangePassword(true);
        admin.setPrimaryAdmin(false);
        admin.setFailedLoginAttempts(0);
        admin.setCreatedAt(LocalDateTime.now());
        try {
            return UserMapper.toResponse(userRepo.saveAndFlush(admin));
        } catch (DataIntegrityViolationException ex) {
            // Two creations with the same details at once: the unique constraints stopped the second
            rejectDuplicates(email, username, mobile);
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "The admin could not be created. Please try again.");
        }
    }

    @Override
    @Auditable(action = "ADMIN_STATUS_CHANGED", resource = "User")
    public UserResponse setAdminActive(Long actingUserId, Long adminId, boolean active) {
        requirePrimaryAdmin(actingUserId);
        User target = userRepo.findById(adminId)
                .filter(user -> UserRole.ADMIN.getLabel().equals(user.getUserRole()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Admin not found."));
        if (target.isPrimaryAdminAccount()) {
            throw new BusinessException(ErrorCode.PRIMARY_ADMIN_PROTECTED, "The primary admin cannot be disabled.");
        }
        target.setAccountStatus(active ? AccountStatus.ACTIVE : AccountStatus.DISABLED);
        return UserMapper.toResponse(userRepo.save(target));
    }

    private void requirePrimaryAdmin(Long actingUserId) {
        boolean primary = userRepo.findById(actingUserId).map(User::isPrimaryAdminAccount).orElse(false);
        if (!primary) {
            throw new BusinessException(ErrorCode.PRIMARY_ADMIN_REQUIRED, "Only the primary admin can manage admins.");
        }
    }

    private void rejectDuplicates(String email, String username, String mobile) {
        if (userRepo.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "An account with this email already exists.");
        }
        if (userRepo.existsByMobileNumber(mobile)) {
            throw new BusinessException(ErrorCode.DUPLICATE_MOBILE, "An account with this mobile number already exists.");
        }
        if (userRepo.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME, "This username is already taken.");
        }
    }
}