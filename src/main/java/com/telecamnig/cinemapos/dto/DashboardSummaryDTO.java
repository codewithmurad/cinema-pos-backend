// DashboardSummaryDTO.java
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
public class DashboardSummaryDTO {
    
	private BigDecimal totalRevenue;      // Sum of grand_total (PAID orders only)
    
	private BigDecimal foodSales;         // Sum of sub_total
    
	private BigDecimal vatCollected;      // Sum of vat_amount
    
	private Long totalOrders;             // Count of orders
    
	private BigDecimal averageOrderValue; // totalRevenue / totalOrders

}