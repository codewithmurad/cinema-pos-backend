package com.telecamnig.cinemapos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents one food item selected in billing.
 */
@Getter
@Setter
public class FoodItemRequest {

    @NotBlank(message = "Food publicId is required")
    private String foodPublicId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

}
