package com.telecamnig.cinemapos.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Response wrapper for returning movie lists. Extends CommonApiResponse like
 * your UsersResponse pattern.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class MoviesResponse extends CommonApiResponse {

	private List<MovieDto> movies;

	// pagination metadata (null when not applicable)
	private Integer page;
	
	private Integer size;
	
	private Long totalElements;
	
	private Integer totalPages;

}
