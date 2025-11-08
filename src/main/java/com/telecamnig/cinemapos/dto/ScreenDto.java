package com.telecamnig.cinemapos.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for screen information in show responses.
 */
@Data
@Builder
public class ScreenDto {
	
	private String publicId;
	
	private String code;
	
	private String name;
	
	private String category; // Regular, Premium, Gold, VIP
	
	private String layoutJson; // Screen layout structure
	
	private Integer totalSeats;
	
	private Integer capacity;

}