package com.telecamnig.cinemapos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telecamnig.cinemapos.dto.ScreenDetailResponse;
import com.telecamnig.cinemapos.dto.ScreenListResponse;
import com.telecamnig.cinemapos.service.ScreenService;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/screens")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    /**
     * Get all active screens with complete seat information
     * Accessible to authenticated users
     */
//    @PreAuthorize("isAuthenticated()")
    @GetMapping("/active")
    public ResponseEntity<ScreenListResponse> getActiveScreens() {
        return screenService.getAllActiveScreens();
    }

    /**
     * Get screen by public ID with complete seat information
     * Accessible to authenticated users
     */
    //@PreAuthorize("isAuthenticated()")
    @GetMapping("/{publicId}")
    public ResponseEntity<ScreenDetailResponse> getScreenByPublicId(
            @PathVariable("publicId") @NotBlank String publicId) {
        return screenService.getScreenByPublicId(publicId);
    }
    
    /**
     * Get screen by code with complete seat information
     * Accessible to authenticated users
     */
    //@PreAuthorize("isAuthenticated()")
    @GetMapping("/code/{code}")
    public ResponseEntity<ScreenDetailResponse> getScreenByCode(
            @PathVariable("code") @NotBlank String code) {
        return screenService.getScreenByCode(code);
    }

    /**
     * Get all screens (including inactive) with complete seat information
     * Admin only access
     */
//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ScreenListResponse> getAllScreens() {
        return screenService.getAllScreens();
    }
    
}