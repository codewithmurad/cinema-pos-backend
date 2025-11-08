package com.telecamnig.cinemapos.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * DTO containing complete show details including movie and screen information.
 * Used for show detail pages and admin views.
 */
@Data
@Builder
public class ShowDetailDto {
	
	private String publicId;
	
	private MovieDto movie; // Complete movie details
	
	private ScreenDto screen; // Complete screen details
	
	private LocalDateTime startAt;
	
	private LocalDateTime endAt;
	
	private Integer status;
	
	private String statusLabel; // Human-readable status
	
	private Integer availableSeats;
	
	private Integer totalSeats;
	
	private Integer bookedSeats;
	
	private BigDecimal minPrice; // Minimum seat price for this show
	
	private BigDecimal maxPrice; // Maximum seat price for this show
	
	private LocalDateTime createdAt;

}