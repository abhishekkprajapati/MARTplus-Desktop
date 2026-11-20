package com.martplus.desktop.controller;

import com.martplus.desktop.model.User;
import com.martplus.desktop.service.AuthenticationService;

/**
 * Controller mediating user registration actions between Register UI and AuthenticationService.
 */
public class RegistrationController {

    private final AuthenticationService authService;

    public RegistrationController(AuthenticationService authService) {
        this.authService = authService;
    }

    public User handleRegister(String firstName, String lastName, String email, String phone,
                               String password, String confirmPassword, String role) {
        return authService.register(firstName, lastName, email, phone, password, confirmPassword, role);
    }
}

