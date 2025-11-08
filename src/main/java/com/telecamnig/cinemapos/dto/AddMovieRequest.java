package com.telecamnig.cinemapos.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for adding a movie. Strict validation added for production-level safety.
 */
@Getter
@Setter
public class AddMovieRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;
    
    @Size(max = 5000, message = "description must be at most 5000 characters")
    private String description;

    @NotNull(message = "durationMinutes is required")
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

    // ❌ REMOVED: posterPath - Now handled by MultipartFile
    // @Size(max = 500, message = "posterPath must be at most 500 characters")
    // private String posterPath;

    private Boolean is3D = Boolean.FALSE;
    private Boolean isIMAX = Boolean.FALSE;
    private Boolean subtitlesAvailable = Boolean.FALSE;

    @Size(max = 500, message = "audioLanguages must be at most 500 characters")
    private String audioLanguages;

    @DecimalMin(value = "0.0", inclusive = true, message = "rating must be >= 0.0")
    @DecimalMax(value = "10.0", inclusive = true, message = "rating must be <= 10.0")
    private Double rating;

    @Size(max = 20, message = "parentalRating must be at most 20 characters")
    private String parentalRating;

    /**
     * releaseDate is optional (admin may set). Accept LocalDateTime to match entity design.
     */
    private LocalDateTime releaseDate;

    /**
     * Status codes you use in system. Enforce bounds to prevent invalid codes.
     * 0 = UPCOMING, 1 = ACTIVE, 2 = INACTIVE, 3 = ARCHIVED, 4 = DELETED
     */
    @NotNull(message = "status is required")
    @Min(value = 0, message = "invalid status code")
    @Max(value = 4, message = "invalid status code")
    private Integer status;
    
}