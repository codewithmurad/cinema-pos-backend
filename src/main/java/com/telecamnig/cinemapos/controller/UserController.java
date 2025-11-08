package com.telecamnig.cinemapos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.UpdateUserStatusRequest;
import com.telecamnig.cinemapos.dto.UserResponse;
import com.telecamnig.cinemapos.dto.UsersResponse;
import com.telecamnig.cinemapos.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get user info by public userId (UUID-like).
     * Requires authentication.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserByPublicId(
    		@PathVariable("userId") @NotBlank String userId) {
        return userService.getUserByPublicId(userId);
    }
    
    /**
     * GET /api/v1/users?role=ADMIN&status=1
     * role is required, status optional
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<UsersResponse> getUsersByRoleAndStatus(
            @RequestParam("role") String role,
            @RequestParam(value = "status", required = false) Integer status) {
        return userService.getUsersByRoleAndStatus(role, status);
    }
    
    /**
     * Update user status (ADMIN only).
     * PUT /api/v1/users/{userId}/status
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/update/status")
    public ResponseEntity<CommonApiResponse> updateUserStatus(
            @PathVariable("userId") @NotBlank String userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateUserStatus(userId, request.getStatus());
    }


}
