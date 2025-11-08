package com.telecamnig.cinemapos.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Lightweight DTO for show listings with essential information.
 * Optimized for performance in list views.
 */
@Data
@Builder
public class ShowListItemDto {
	
    private String publicId;
    
    private String moviePublicId;
    
    private String movieTitle;
    
    private String posterPath;
    
    private Integer durationMinutes;
    
    private String screenPublicId;
    
    private String screenName;
    
    private String screenCategory;
    
    private LocalDateTime startAt;
    
    private LocalDateTime endAt;
    
    private Integer status;
    
    private String statusLabel;
    
    private Integer availableSeats;
    
    private Integer totalSeats;
    
    private BigDecimal minPrice;
    
    private BigDecimal maxPrice;
    
    private LocalDateTime createdAt;

}