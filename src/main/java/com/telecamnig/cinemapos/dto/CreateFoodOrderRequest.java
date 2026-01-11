package com.telecamnig.cinemapos.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating a food bill.
 */
@Getter
@Setter
public class CreateFoodOrderRequest {

    /**
     * List of food items selected for billing.
     */
    @NotEmpty(message = "Food items are required")
    private List<FoodItemRequest> items;

    /**
     * Payment mode: CASH / CARD / UPI
     */
    @NotNull(message = "Payment mode is required")
    private String paymentMode;

}
