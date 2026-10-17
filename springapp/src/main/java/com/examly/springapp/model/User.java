package com.examly.springapp.model;

import java.time.LocalDateTime;

import com.examly.springapp.model.enums.AccountStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An account (Admin or User). Phase 2:
 *  - email, mobileNumber and username are UNIQUE in the database (protects against two
 *    simultaneous signups with the same details - an application check alone cannot)
 *  - input validation lives on the request DTOs (dto/auth), not here
 *  - new columns are nullable so existing Phase 1 rows stay valid; null means "legacy account":
 *    active and verified
 * Controllers never return this entity directly (UserResponse is used instead). Until the request
 * and review DTOs arrive in Steps 4 and 6, it is still embedded there, so every sensitive field is
 * hidden from JSON.
 */
@Entity
@Table(name = "users")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(unique = true, length = 10)
    private String mobileNumber;

    /** "Admin" or "User" (UserRole labels). Assigned by the server only, never by signup. */
    @Column(nullable = false)
    private String userRole;

    @Column(length = 300)
    private String address;

    private Boolean emailVerified;

    private Boolean mobileVerified;

    private AccountStatus accountStatus;

    @JsonIgnore
    private Boolean mustChangePassword;

    private Boolean primaryAdmin;

    @JsonIgnore
    private Integer failedLoginAttempts;

    @JsonIgnore
    private LocalDateTime lockedUntil;

    private LocalDateTime createdAt;

    public User() {
        // Required by JPA and Jackson
    }

    /** Legacy (Phase 1) rows have no status: treat them as active. */
    @JsonIgnore
    public boolean isActive() {
        return accountStatus == null || accountStatus == AccountStatus.ACTIVE;
    }

    @JsonIgnore
    public boolean isPasswordChangeRequired() {
        return Boolean.TRUE.equals(mustChangePassword);
    }

    @JsonIgnore
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    @JsonIgnore
    public boolean isPrimaryAdminAccount() {
        return Boolean.TRUE.equals(primaryAdmin);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Boolean getMobileVerified() {
        return mobileVerified;
    }

    public void setMobileVerified(Boolean mobileVerified) {
        this.mobileVerified = mobileVerified;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public Boolean getPrimaryAdmin() {
        return primaryAdmin;
    }

    public void setPrimaryAdmin(Boolean primaryAdmin) {
        this.primaryAdmin = primaryAdmin;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}