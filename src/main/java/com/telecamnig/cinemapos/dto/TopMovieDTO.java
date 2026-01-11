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
public class TopMovieDTO {
    private String movieTitle;
    private Long ticketsSold;
    private BigDecimal totalRevenue;
}