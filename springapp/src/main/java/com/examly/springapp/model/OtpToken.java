package com.examly.springapp.model;

import java.time.LocalDateTime;

import com.examly.springapp.model.enums.OtpChannel;
import com.examly.springapp.model.enums.OtpPurpose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One issued one-time password. Only a BCrypt HASH of the code is stored - never the code itself.
 * A token is single-use (consumed), expires, and allows a limited number of wrong guesses.
 */
@Entity
@Table(name = "OtpToken", indexes = @Index(name = "idx_otp_user_channel_purpose",
        columnList = "userId, channel, purpose"))
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long otpId;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(nullable = false)
    private OtpChannel channel;

    @Column(nullable = false)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean consumed;

    public OtpToken() {
        // Required by JPA
    }

    public OtpToken(User user, OtpChannel channel, OtpPurpose purpose, String codeHash, LocalDateTime expiresAt) {
        this.user = user;
        this.channel = channel;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public Long getOtpId() {
        return otpId;
    }

    public User getUser() {
        return user;
    }

    public OtpChannel getChannel() {
        return channel;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isConsumed() {
        return consumed;
    }
}