// TopSellingItemDTO.java
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
public class TopSellingItemDTO {
	
    private String foodName;
    
    private String categoryName;
    
    private Long totalQuantity;      // Total quantity sold
    
    private BigDecimal totalAmount;  // Total revenue from this item

}