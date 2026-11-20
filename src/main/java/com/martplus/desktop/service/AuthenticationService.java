package com.martplus.desktop.service;

import com.martplus.desktop.config.AppConfig;
import com.martplus.desktop.dao.UserDao;
import com.martplus.desktop.exception.AuthenticationException;
import com.martplus.desktop.exception.ValidationException;
import com.martplus.desktop.model.User;
import com.martplus.desktop.util.PasswordUtil;
import com.martplus.desktop.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service handling user authentication and session management.
 * Verifies passwords against BCrypt hashes stored in PostgreSQL.
 */
public class AuthenticationService {

    private final UserDao userDao;

    public AuthenticationService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Authenticates a user against PostgreSQL records using BCrypt hashing.
     *
     * @param email User email
     * @param rawPassword Plaintext password from UI
     * @param expectedRole Selected role (BUYER, SELLER, ADMIN)
     * @return Authenticated User model
     */
    public User login(String email, String rawPassword, String expectedRole) {
        ValidationUtil.validateEmail(email);
        ValidationUtil.requireNonEmpty(rawPassword, "Password");
        ValidationUtil.requireNonEmpty(expectedRole, "Account Type");

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedRole = expectedRole.trim().toUpperCase();

        Optional<User> userOpt = userDao.findByEmailAndRole(normalizedEmail, normalizedRole);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("No account found with this email and role (" + normalizedRole + ").");
        }

        User user = userOpt.get();

        // Check password against BCrypt hash
        if (!PasswordUtil.checkPassword(rawPassword, user.getPassword())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        // Mirror backend behavior: if PENDING_VERIFICATION, activate account upon valid login
        if ("PENDING_VERIFICATION".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("ACTIVE");
            user.setEmailVerified(true);
            user.setPhoneVerified(true);
            userDao.updateStatus(user.getId(), "ACTIVE");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Your account is " + user.getStatus() + ". Please contact administrator.");
        }

        // Establish session in AppConfig
        AppConfig.setCurrentUser(user);
        return user;
    }

    public void logout() {
        AppConfig.clearSession();
    }

    /**
     * Registers a new user account (BUYER or SELLER) using direct JDBC and BCrypt hashing.
     * Enforces all client/database business validation rules.
     */
    public User register(String firstName, String lastName, String email, String phone,
                         String rawPassword, String confirmPassword, String role) {
        // 1. Required fields non-empty
        ValidationUtil.requireNonEmpty(firstName, "First name");
        ValidationUtil.requireNonEmpty(lastName, "Last name");
        ValidationUtil.requireNonEmpty(email, "Email address");
        ValidationUtil.requireNonEmpty(phone, "Phone number");
        ValidationUtil.requireNonEmpty(rawPassword, "Password");
        ValidationUtil.requireNonEmpty(confirmPassword, "Confirm password");
        ValidationUtil.requireNonEmpty(role, "Account Type");

        // 2. Email format validation
        ValidationUtil.validateEmail(email);
        String normalizedEmail = email.trim().toLowerCase();

        // 3. Phone validation (10 digits)
        ValidationUtil.validatePhone(phone);
        String normalizedPhone = phone.trim();

        // 4. Role validation - only BUYER or SELLER allowed (prevent ADMIN self-registration)
        String normalizedRole = role.trim().toUpperCase();
        if ("ADMIN".equalsIgnoreCase(normalizedRole)) {
            throw new ValidationException("Self-registration as ADMIN is strictly prohibited.");
        }
        if (!"BUYER".equals(normalizedRole) && !"SELLER".equals(normalizedRole)) {
            throw new ValidationException("Account type must be either BUYER or SELLER.");
        }

        // 5. Password minimum length & confirmation matching
        if (rawPassword.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        if (!rawPassword.equals(confirmPassword)) {
            throw new ValidationException("Password and Confirm Password do not match.");
        }

        // 6. Duplicate email + role rejection
        if (userDao.findByEmailAndRole(normalizedEmail, normalizedRole).isPresent()) {
            throw new ValidationException("An account with this email is already registered as " + normalizedRole + ".");
        }

        // 7. Duplicate phone + role rejection (matching backend)
        if (userDao.findByPhoneAndRole(normalizedPhone, normalizedRole).isPresent()) {
            throw new ValidationException("Phone number already registered for " + normalizedRole.toLowerCase() + " account.");
        }

        // 8. Hash password using existing BCrypt PasswordUtil
        String hashedPassword = PasswordUtil.hashPassword(rawPassword);

        // 9. Build User model with ACTIVE status and verified flags matching web app
        User user = new User(
                null,
                firstName.trim(),
                lastName.trim(),
                normalizedEmail,
                normalizedPhone,
                hashedPassword,
                normalizedRole,
                "ACTIVE",
                true,
                true,
                LocalDateTime.now()
        );

        // 10. Persist via Direct JDBC DAO
        return userDao.save(user);
    }
}
