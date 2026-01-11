package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDTO {
    // Existing food fields
    private DashboardSummaryDTO summary;
    private DashboardPaymentSummaryDTO paymentSummary;
    private List<TopSellingItemDTO> topSellingItems;
    
    // NEW: Add these ticket fields
    private TicketDashboardSummaryDTO ticketSummary;
    private TicketPaymentSummaryDTO ticketPaymentSummary;
    private List<TopMovieDTO> topMovies;
}