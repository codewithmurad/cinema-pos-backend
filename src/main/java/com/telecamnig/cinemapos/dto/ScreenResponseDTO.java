package com.telecamnig.cinemapos.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO for screen response - contains complete screen information with seats
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScreenResponseDTO {
    
	private String publicId;
    
	private String code;
    
	private String name;
    
	private String category;
    
	private JsonNode layoutJson; // Parsed JSON for frontend layout
    
	private int status;
    
	private LocalDateTime createdAt;
    
	private LocalDateTime updatedAt;
    
    // Seat information
    private List<ScreenSeatResponseDTO> seats;
    
    private Integer totalSeats;
    
    private Integer availableSeats;
    
    // Screen statistics
    private Integer regularSeats;
    
    private Integer goldSeats;
    
    private Integer premiumSeats;
    
    private Integer sofaSeats;

}