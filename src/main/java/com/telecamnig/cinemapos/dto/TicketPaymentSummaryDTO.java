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
public class TicketPaymentSummaryDTO {
    private BigDecimal cashRevenue;
    private BigDecimal cashVat;
    private Long cashTicketCount;
    
    private BigDecimal posRevenue;
    private BigDecimal posVat;
    private Long posTicketCount;
  
}