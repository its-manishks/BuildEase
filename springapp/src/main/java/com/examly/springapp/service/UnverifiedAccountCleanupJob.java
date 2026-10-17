package com.examly.springapp.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.model.enums.AccountStatus;
import com.examly.springapp.repository.OtpTokenRepo;
import com.examly.springapp.repository.UserRepo;

/**
 * Every hour, deletes signups that were never verified within app.accounts.unverified-ttl-hours
 * (default 24). This frees their email, mobile and username so a real owner can register them,
 * and stops abandoned or fake signups from piling up. Unverified accounts cannot log in, so they
 * never own requests or reviews.
 */
@Component
public class UnverifiedAccountCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnverifiedAccountCleanupJob.class);

    private final UserRepo userRepo;
    private final OtpTokenRepo otpTokenRepo;
    private final long ttlHours;

    public UnverifiedAccountCleanupJob(UserRepo userRepo, OtpTokenRepo otpTokenRepo,
                                       @Value("${app.accounts.unverified-ttl-hours:24}") long ttlHours) {
        this.userRepo = userRepo;
        this.otpTokenRepo = otpTokenRepo;
        this.ttlHours = ttlHours;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
    @Transactional
    public void purgeStaleSignups() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(ttlHours);
        otpTokenRepo.deleteForStaleAccounts(AccountStatus.PENDING_VERIFICATION, cutoff);
        int removed = userRepo.deleteStaleAccounts(AccountStatus.PENDING_VERIFICATION, cutoff);
        if (removed > 0) {
            LOGGER.info("Removed {} unverified signup(s) older than {} hours", removed, ttlHours);
        }
    }
}