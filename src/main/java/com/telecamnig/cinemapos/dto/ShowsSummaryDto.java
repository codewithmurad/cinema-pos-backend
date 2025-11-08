package com.telecamnig.cinemapos.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Summary statistics for show listings. Provides quick overview for dashboard
 * views.
 */
@Data
@Builder
public class ShowsSummaryDto {

	private Integer totalShows;

	private Integer upcomingShows;

	private Integer runningShows;

	private Integer completedShows;

	private Integer cancelledShows;

	private Integer totalAvailableSeats;

	private Map<String, Integer> showsByScreen; // Screen code -> count

	private Map<String, Integer> showsByMovie; // Movie title -> count

}