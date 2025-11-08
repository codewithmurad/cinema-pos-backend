package com.telecamnig.cinemapos.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * Request DTO for searching and filtering shows. Supports multiple filter
 * criteria for flexible search.
 */
@Data
public class ShowSearchRequest {

	private String moviePublicId; // Filter by specific movie

	private String screenPublicId; // Filter by specific screen

	private LocalDate date; // Filter by specific date

	private Integer status; // Filter by show status

	private LocalDate startDate; // Date range start

	private LocalDate endDate; // Date range end

	// Pagination
	private Integer page = 0;

	private Integer size = 20;

}