package com.examly.springapp.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.examly.springapp.model.User;
import com.examly.springapp.model.enums.AccountStatus;
import com.examly.springapp.model.enums.UserRole;
import com.examly.springapp.repository.UserRepo;

/**
 * Creates the one primary admin on the first start (username "admin", password from
 * ADMIN_SEED_PASSWORD, default Admin@123). The password is stored as a BCrypt hash and MUST be
 * changed at first login - every other API is blocked for this account until it is.
 * Runs on every start but only acts when no primary admin exists, so it never resets a password.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminSeeder.class);
    /** Audit only real creations (not every restart), in the same format as AuditAspect. */
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;
    private final String mobile;

    public AdminSeeder(UserRepo userRepo, PasswordEncoder passwordEncoder,
                       @Value("${app.admin.seed.username}") String username,
                       @Value("${app.admin.seed.password}") String password,
                       @Value("${app.admin.seed.email}") String email,
                       @Value("${app.admin.seed.mobile}") String mobile) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.email = email;
        this.mobile = mobile;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepo.existsByPrimaryAdminTrue()) {
            return;
        }
        if (userRepo.existsByUsernameIgnoreCase(username) || userRepo.existsByEmail(email)
                || userRepo.existsByMobileNumber(mobile)) {
            LOGGER.warn("Primary admin not seeded: the seed username, email or mobile is already used by another "
                    + "account. Reset the development database or change the app.admin.seed.* settings.");
            return;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setMobileNumber(mobile);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setUserRole(UserRole.ADMIN.getLabel());
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setEmailVerified(true);
        admin.setMobileVerified(true);
        admin.setMustChangePassword(true);
        admin.setPrimaryAdmin(true);
        admin.setFailedLoginAttempts(0);
        admin.setCreatedAt(LocalDateTime.now());
        User saved = userRepo.save(admin);
        LOGGER.info("Primary admin '{}' created. The password must be changed at first login.", username);
        AUDIT.info("action=PRIMARY_ADMIN_SEEDED outcome=SUCCESS actor=system resource=User target={} ip=system",
                saved.getUserId());
    }
}