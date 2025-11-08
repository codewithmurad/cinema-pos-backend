package com.telecamnig.cinemapos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telecamnig.cinemapos.dto.AdminRegisterRequest;
import com.telecamnig.cinemapos.dto.ChangePasswordRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.CounterRegisterRequest;
import com.telecamnig.cinemapos.dto.ForgotPasswordRequest;
import com.telecamnig.cinemapos.dto.LoginRequest;
import com.telecamnig.cinemapos.dto.LoginResponse;
import com.telecamnig.cinemapos.service.AuthService;

import jakarta.validation.Valid;

/**
 * Authentication & User Registration Controller
 * Delegates business logic to AuthService. Controller methods simply forward responses.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Login endpoint (public)
     * Delegates to AuthService which returns ResponseEntity<LoginResponse>.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Register a new admin user.
     * Accessible only to existing admins.
     * Delegates to AuthService which returns ResponseEntity<CommonApiResponse>.
     */
//   @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register/admin")
    public ResponseEntity<CommonApiResponse> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        return authService.registerAdmin(request);
    }

    /**
     * Register a new counter staff user.
     * Accessible only to admins.
     * Delegates to AuthService which returns ResponseEntity<CommonApiResponse>.
     */
//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register/counter")
    public ResponseEntity<CommonApiResponse> registerCounter(@Valid @RequestBody CounterRegisterRequest request) {
        return authService.registerCounter(request);
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<CommonApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PreAuthorize("isAuthenticated()")
//    @PostMapping("/change-password")
    public ResponseEntity<CommonApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }

}
