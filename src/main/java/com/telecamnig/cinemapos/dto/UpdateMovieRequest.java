package com.telecamnig.cinemapos.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for updating movie details (poster excluded).
 * All fields are optional — only non-null fields will be updated.
 */
@Getter
@Setter
public class UpdateMovieRequest {

    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    @Size(max = 5000, message = "description must be at most 5000 characters")
    private String description;

    @Min(value = 1, message = "durationMinutes must be >= 1")
    @Max(value = 1000, message = "durationMinutes seems too large")
    private Integer durationMinutes;

    @Size(max = 500, message = "genres must be at most 500 characters")
    private String genres;

    @Size(max = 100, message = "language must be at most 100 characters")
    private String language;

    @Size(max = 100, message = "country must be at most 100 characters")
    private String country;

    @Size(max = 250, message = "director must be at most 250 characters")
    private String director;

    @Size(max = 2000, message = "castMembers must be at most 2000 characters")
    private String castMembers;

    @Size(max = 2000, message = "producers must be at most 2000 characters")
    private String producers;

    @Size(max = 250, message = "productionCompany must be at most 250 characters")
    private String productionCompany;

    @Size(max = 250, message = "distributor must be at most 250 characters")
    private String distributor;

    @Size(max = 500, message = "audioLanguages must be at most 500 characters")
    private String audioLanguages;

    @DecimalMin(value = "0.0", inclusive = true, message = "rating must be >= 0.0")
    @DecimalMax(value = "10.0", inclusive = true, message = "rating must be <= 10.0")
    private Double rating;

    @Size(max = 20, message = "parentalRating must be at most 20 characters")
    private String parentalRating;

    /**
     * releaseDate may be updated by admin to correct metadata; keep optional.
     */
    private LocalDateTime releaseDate;

    /** Flags */
    private Boolean is3D;
    
    private Boolean isIMAX;
    
    private Boolean subtitlesAvailable;

}
