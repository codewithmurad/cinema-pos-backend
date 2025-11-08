package com.telecamnig.cinemapos.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO representing a movie entity. Used inside MoviesResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieDto {

	private String publicId;

	private String title;
	
	private String description;

	private Integer durationMinutes;

	private String genres;

	private String language;

	private String country;

	private String director;

	private String castMembers;

	private String producers;

	private String productionCompany;

	private String distributor;

	private String posterPath;

	private Boolean is3D;

	private Boolean isIMAX;

	private Boolean subtitlesAvailable;

	private String audioLanguages;

	private Double rating;

	private String parentalRating;

	private LocalDateTime releaseDate;

	private int status;

	private LocalDateTime createdAt;

}
