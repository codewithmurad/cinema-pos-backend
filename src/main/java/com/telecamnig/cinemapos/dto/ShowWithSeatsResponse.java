package com.telecamnig.cinemapos.dto;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Response wrapper for show details with seat information.
 * Used for APIs that return both show metadata and seat availability.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ShowWithSeatsResponse extends CommonApiResponse {
 
	private ShowDetailDto show;
    
	private List<ShowSeatDto> seats;
    
    private SeatMapSummaryDto seatSummary;

}