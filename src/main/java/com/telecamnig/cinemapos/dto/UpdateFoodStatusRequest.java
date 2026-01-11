package com.telecamnig.cinemapos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for updating Food status.
 */
@Getter
@Setter
public class UpdateFoodStatusRequest {

    /**
     * FoodStatus codes:
     * 0 = INACTIVE
     * 1 = ACTIVE
     * 2 = DELETED
     */
    @NotNull(message = "status is required")
    @Min(value = 0, message = "invalid status code")
    @Max(value = 2, message = "invalid status code")
    private Integer status;
    
}
