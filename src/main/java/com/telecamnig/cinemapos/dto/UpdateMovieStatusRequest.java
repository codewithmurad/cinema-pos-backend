package com.telecamnig.cinemapos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for updating a movie status.
 * Any authenticated user can call this endpoint.
 */
@Getter
@Setter
public class UpdateMovieStatusRequest {

    @NotNull(message = "status is required")
    @Min(value = 0, message = "invalid status code")
    @Max(value = 4, message = "invalid status code")
    private Integer status;
    
}
