package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for show seat information including availability state.
 * Used for seat selection and seat map rendering.
 */
@Data
@Builder
public class ShowSeatDto {
	
    private String publicId;
    
    private String seatPublicId;        // Reference to original ScreenSeat
    
    private String seatLabel;           // e.g., "A1", "B2"
    
    private String seatType;            // REGULAR, PREMIUM, GOLD, VIP_SOFA
    
    private String seatTypeDisplay;     // "Regular", "Premium", etc.
    
    private String state;               // AVAILABLE, HELD, SOLD
    
    private BigDecimal price;
    
    private Integer rowIndex;
    
    private Integer colIndex;
    
    private String groupPublicId;       // For sofa groups (VIP screens)
    
    private Map<String, Object> layoutMeta; // Parsed seat position metadata
    
    private Boolean isSelectable;       // Frontend helper - based on state

}