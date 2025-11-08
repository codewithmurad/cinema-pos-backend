package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScheduleShowRequest {

	@NotBlank(message = "Movie public ID is required")
	private String moviePublicId;

	@NotBlank(message = "Screen public ID is required")
	private String screenPublicId;

	@NotNull(message = "Show start time is required")
	@Future(message = "Show start time must be in the future")
	private LocalDateTime startAt;

	@NotNull(message = "Seat prices are required")
	private Map<String, BigDecimal> seatPrices; // {"Regular": 8500, "Gold": 12000, "Premium": 15000, "VIP Sofa": 20000}

	private Integer status; // Optional manual status override

}