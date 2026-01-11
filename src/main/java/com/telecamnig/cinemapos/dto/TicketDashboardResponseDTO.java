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
public class TicketDashboardResponseDTO {
    private TicketDashboardSummaryDTO summary;
    private TicketPaymentSummaryDTO paymentSummary;
    private List<TopMovieDTO> topMovies;
}