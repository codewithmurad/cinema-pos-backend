package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatisticsResponse extends CommonApiResponse {
    
	private long totalBookings;
    
	private long todayBookings;
    
	private BigDecimal totalRevenue;
    
	private BigDecimal todayRevenue;
    
	private long cancelledBookings;
    
	private long refundedBookings;
    
	private PaymentMethodStats paymentMethodStats;

}