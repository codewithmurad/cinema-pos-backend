package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDTO {
    
	private String bookingPublicId;
	
    private String bookingGroupRef;
    
	private String showPublicId;
    
	private String movieTitle;
    
	private String screenName;
    
	private BigDecimal baseAmount;
	
    private BigDecimal vatAmount;
    
    private BigDecimal totalAmount;
    
	private String paymentMode;
    
	private String transactionReference;
    
	private String status;
    
	private LocalDateTime bookedAt;
    
	private LocalDateTime showStartTime;
    
	private LocalDateTime showEndTime;
    
	private List<String> seatLabels;
    
	private Integer printCount;

    private String qrCodeData;

}