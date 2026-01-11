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
public class TicketDashboardSummaryDTO {
    private BigDecimal totalRevenue;
    private Long ticketsSold;
    private BigDecimal vatCollected;
    private BigDecimal avgTicketPrice;
    private BigDecimal occupancyRate;
}