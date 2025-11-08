package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Summary statistics for seat map display.
 * Used in seat selection UI to show quick overview.
 */
@Data
@Builder
public class SeatMapSummaryDto {
	
    private Integer totalSeats;
    
    private Integer availableSeats;
    
    private Integer heldSeats;
    
    private Integer soldSeats;
    
    private BigDecimal minPrice;
    
    private BigDecimal maxPrice;
    
    private Map<String, Integer> seatsByType; // {"REGULAR": 20, "PREMIUM": 15}

}