package com.telecamnig.cinemapos.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookingListResponse extends CommonApiResponse {
 
	private List<BookingResponseDTO> bookings;
    
	private PaginationInfo pagination;

}