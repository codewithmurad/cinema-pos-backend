package com.telecamnig.cinemapos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO for screen seat response - contains all seat information for frontend
 * rendering
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScreenSeatResponseDTO {
	
	private String publicId;
	
	private String label;
	
	private Integer rowIndex;
	
	private Integer colIndex;
	
	private String seatType;
	
	private String groupPublicId; // For VIP sofa grouping
	
	private Object metaJson; // Parsed JSON for frontend coordinates
	
	private int status;

}