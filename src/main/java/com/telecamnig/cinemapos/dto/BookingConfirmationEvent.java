package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingConfirmationEvent {
    
    private String bookingPublicId;
    
    private String showPublicId;
    
    private String customerName;  // Add this field
    
    private List<String> seatLabels;
    
    private BigDecimal totalAmount;  // Change from Double to BigDecimal
    
    private String paymentMode;
    
    private LocalDateTime timestamp;  // Change from bookingTime to timestamp
    
    private String bookedBy;  // Keep this if needed
    
    // Optional: Add these fields for completeness
    private String movieTitle;
    
    private String screenName;
    
    private LocalDateTime showTime;

}