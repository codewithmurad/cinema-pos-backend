package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor  
public class PaymentMethodStats {
  
	private long cashCount;
    
	private long posCount;
    
	private long transferCount;
    
	private long onlineCount;
    
	private BigDecimal cashAmount;
    
	private BigDecimal posAmount;
    
	private BigDecimal transferAmount;
    
	private BigDecimal onlineAmount;

}