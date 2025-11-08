package com.telecamnig.cinemapos.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecamnig.cinemapos.dto.ScreenDetailResponse;
import com.telecamnig.cinemapos.dto.ScreenListResponse;
import com.telecamnig.cinemapos.dto.ScreenResponseDTO;
import com.telecamnig.cinemapos.dto.ScreenSeatResponseDTO;
import com.telecamnig.cinemapos.entity.Screen;
import com.telecamnig.cinemapos.entity.ScreenSeat;
import com.telecamnig.cinemapos.repository.ScreenRepository;
import com.telecamnig.cinemapos.repository.ScreenSeatRepository;
import com.telecamnig.cinemapos.service.ScreenService;
import com.telecamnig.cinemapos.utility.ApiResponseMessage;
import com.telecamnig.cinemapos.utility.Constants.ScreenStatus;
import com.telecamnig.cinemapos.utility.Constants.SeatStatus;
import com.telecamnig.cinemapos.utility.Constants.SeatType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;
    private final ScreenSeatRepository screenSeatRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ResponseEntity<ScreenListResponse> getAllActiveScreens() {
        log.info("Service: Getting all active screens");
        
        try {
            List<Screen> activeScreens = screenRepository.findByStatus(ScreenStatus.ACTIVE.getCode());
            
            // Fast conversion - trust our data quality
            List<ScreenResponseDTO> screenDTOs = activeScreens.stream()
                    .map(this::convertToScreenResponseDTO)
                    .collect(Collectors.toList());
            
            String message = screenDTOs.isEmpty() ? 
                ApiResponseMessage.NO_DATA_FOUND : 
                "Active screens retrieved successfully";
                
            return ResponseEntity.ok(ScreenListResponse.builder()
                    .success(true)
                    .message(message)
                    .screens(screenDTOs)
                    .build());
            
        } catch (Exception e) {
            log.error("Service error retrieving active screens", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ScreenListResponse.builder()
                            .success(false)
                            .message("Failed to retrieve active screens")
                            .screens(Collections.emptyList())
                            .build());
        }
    }

    @Override
    public ResponseEntity<ScreenListResponse> getAllScreens() {
        log.info("Service: Getting all screens");
        
        try {
            List<Screen> allScreens = screenRepository.findAll();
            
            List<ScreenResponseDTO> screenDTOs = allScreens.stream()
                    .map(this::convertToScreenResponseDTO)
                    .collect(Collectors.toList());
            
            String message = screenDTOs.isEmpty() ? 
                ApiResponseMessage.NO_DATA_FOUND : 
                "All screens retrieved successfully";
                
            return ResponseEntity.ok(ScreenListResponse.builder()
                    .success(true)
                    .message(message)
                    .screens(screenDTOs)
                    .build());
            
        } catch (Exception e) {
            log.error("Service error retrieving all screens", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ScreenListResponse.builder()
                            .success(false)
                            .message("Failed to retrieve screens")
                            .screens(Collections.emptyList())
                            .build());
        }
    }

    @Override
    public ResponseEntity<ScreenDetailResponse> getScreenByPublicId(String publicId) {
        log.info("Service: Getting screen by public ID: {}", publicId);
        
        // Only validate user input (publicId)
        if (!StringUtils.hasText(publicId)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiResponseMessage.INVALID_INPUT);
        }
        
        String sanitizedPublicId = publicId.trim();
        
        // Basic UUID format check for user input
        if (!isValidUUID(sanitizedPublicId)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid screen ID format");
        }
        
        try {
            Optional<Screen> screenOpt = screenRepository.findByPublicId(sanitizedPublicId);
            
            if (screenOpt.isPresent()) {
                ScreenResponseDTO screenDTO = convertToScreenResponseDTO(screenOpt.get());
                
                return ResponseEntity.ok(ScreenDetailResponse.builder()
                        .success(true)
                        .message("Screen retrieved successfully")
                        .screen(screenDTO)
                        .build());
            } else {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Screen not found");
            }
            
        } catch (Exception e) {
            log.error("Service error retrieving screen: {}", sanitizedPublicId, e);
            
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve screen");
        }
    }
    
    @Override
    public ResponseEntity<ScreenDetailResponse> getScreenByCode(String code) {
        log.info("Service: Getting screen by code: {}", code);
        
        // Validate user input
        if (!StringUtils.hasText(code)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiResponseMessage.INVALID_INPUT);
        }
        
        String sanitizedCode = code.trim().toUpperCase();
        
        try {
            Optional<Screen> screenOpt = screenRepository.findByCode(sanitizedCode);
            
            if (screenOpt.isPresent()) {
                ScreenResponseDTO screenDTO = convertToScreenResponseDTO(screenOpt.get());
                
                return ResponseEntity.ok(ScreenDetailResponse.builder()
                        .success(true)
                        .message("Screen retrieved successfully")
                        .screen(screenDTO)
                        .build());
            } else {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Screen not found for code: " + sanitizedCode);
            }
            
        } catch (Exception e) {
            log.error("Service error retrieving screen by code: {}", sanitizedCode, e);
            
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve screen");
        }
    }

    /**
     * Convert Screen entity to ScreenResponseDTO with seats and statistics
     */
    private ScreenResponseDTO convertToScreenResponseDTO(Screen screen) {
        try {
            // Get all active seats for this screen
            List<ScreenSeat> screenSeats = screenSeatRepository.findByScreenIdAndStatus(
                screen.getId(), SeatStatus.ACTIVE.getCode());
            
            // Convert seats to DTOs
            List<ScreenSeatResponseDTO> seatDTOs = screenSeats.stream()
                    .map(this::convertToScreenSeatResponseDTO)
                    .collect(Collectors.toList());
            
            // Parse layout JSON
            JsonNode layoutJson = null;
            if (StringUtils.hasText(screen.getLayoutJson())) {
                layoutJson = objectMapper.readTree(screen.getLayoutJson());
            }
            
            // FIXED: Calculate seat statistics with proper type matching
            int regularSeats = (int) seatDTOs.stream()
                    .filter(seat -> SeatType.REGULAR.getValue().equalsIgnoreCase(seat.getSeatType()))
                    .count();
                    
            int goldSeats = (int) seatDTOs.stream()
                    .filter(seat -> SeatType.GOLD.getValue().equalsIgnoreCase(seat.getSeatType()))
                    .count();
                    
            int premiumSeats = (int) seatDTOs.stream()
                    .filter(seat -> SeatType.PREMIUM.getValue().equalsIgnoreCase(seat.getSeatType()))
                    .count();
                    
            int sofaSeats = (int) seatDTOs.stream()
                    .filter(seat -> SeatType.VIP_SOFA.getValue().equalsIgnoreCase(seat.getSeatType()))
                    .count();

            return ScreenResponseDTO.builder()
                    .publicId(screen.getPublicId())
                    .code(screen.getCode())
                    .name(screen.getName())
                    .category(screen.getCategory())
                    .layoutJson(layoutJson)
                    .status(screen.getStatus())
                    .createdAt(screen.getCreatedAt())
                    .updatedAt(screen.getUpdatedAt())
                    .seats(seatDTOs)
                    .totalSeats(seatDTOs.size())
                    .availableSeats(seatDTOs.size()) // All seats are available initially
                    .regularSeats(regularSeats)
                    .goldSeats(goldSeats)
                    .premiumSeats(premiumSeats)
                    .sofaSeats(sofaSeats)
                    .build();
                    
        } catch (JsonProcessingException e) {
            log.error("Error parsing layout JSON for screen: {}", screen.getPublicId(), e);
            throw new RuntimeException("Error processing screen layout data", e);
        }
    }

    /**
     * Fallback method if layout JSON parsing fails
     */
    private ScreenResponseDTO buildScreenResponseWithoutLayout(Screen screen) {
        List<ScreenSeat> screenSeats = screenSeatRepository.findByScreenIdAndStatus(
            screen.getId(), SeatStatus.ACTIVE.getCode());
        
        List<ScreenSeatResponseDTO> seatDTOs = screenSeats.stream()
                .map(this::convertToScreenSeatResponseDTO)
                .collect(Collectors.toList());

        return ScreenResponseDTO.builder()
                .publicId(screen.getPublicId())
                .code(screen.getCode())
                .name(screen.getName())
                .category(screen.getCategory())
                .layoutJson(null) // No layout
                .status(screen.getStatus())
                .createdAt(screen.getCreatedAt())
                .updatedAt(screen.getUpdatedAt())
                .seats(seatDTOs)
                .totalSeats(seatDTOs.size())
                .availableSeats(seatDTOs.size())
                .regularSeats(0) // Skip detailed counts without layout
                .goldSeats(0)
                .premiumSeats(0)
                .sofaSeats(0)
                .build();
    }

    /**
     * Fast seat conversion - trust our data
     */
    private ScreenSeatResponseDTO convertToScreenSeatResponseDTO(ScreenSeat screenSeat) {
        try {
            Object metaJson = null;
            if (StringUtils.hasText(screenSeat.getMetaJson())) {
                metaJson = objectMapper.readValue(screenSeat.getMetaJson(), Object.class);
            }
            
            return ScreenSeatResponseDTO.builder()
                    .publicId(screenSeat.getPublicId())
                    .label(screenSeat.getLabel())
                    .rowIndex(screenSeat.getRowIndex())
                    .colIndex(screenSeat.getColIndex())
                    .seatType(screenSeat.getSeatType())
                    .groupPublicId(screenSeat.getGroupPublicId())
                    .metaJson(metaJson)
                    .status(screenSeat.getStatus())
                    .build();
                    
        } catch (JsonProcessingException e) {
            log.warn("Meta JSON parsing failed for seat: {}", screenSeat.getLabel());
            // Return seat without meta JSON
            return ScreenSeatResponseDTO.builder()
                    .publicId(screenSeat.getPublicId())
                    .label(screenSeat.getLabel())
                    .rowIndex(screenSeat.getRowIndex())
                    .colIndex(screenSeat.getColIndex())
                    .seatType(screenSeat.getSeatType())
                    .groupPublicId(screenSeat.getGroupPublicId())
                    .metaJson(null)
                    .status(screenSeat.getStatus())
                    .build();
        }
    }

    /**
     * Basic UUID validation for user input only
     */
    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Standardized error response builder
     */
    private ResponseEntity<ScreenDetailResponse> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ScreenDetailResponse.builder()
                        .success(false)
                        .message(message)
                        .screen(null)
                        .build());
    }
    
}