// DashboardPaymentSummaryDTO.java
package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPaymentSummaryDTO {
    
	private BigDecimal cashFoodSales;
    
	private BigDecimal cashVatCollected;
    
	private BigDecimal cashTotalRevenue;
    
	private Long cashOrderCount;
    
    private BigDecimal posFoodSales;
    
    private BigDecimal posVatCollected;
    
    private BigDecimal posTotalRevenue;
    
    private Long posOrderCount;

}