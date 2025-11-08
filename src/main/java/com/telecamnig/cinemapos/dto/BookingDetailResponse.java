package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse extends CommonApiResponse {
    
	private BookingResponseDTO booking;

}