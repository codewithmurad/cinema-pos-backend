package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for updating Food item.
 */
@Getter
@Setter
public class UpdateFoodRequest {

    @NotBlank(message = "name must not be blank")
    @Size(max = 150, message = "name must be at most 150 characters")
    private String name;

    /**
     * Category publicId (allows moving food to another category).
     */
    @NotBlank(message = "categoryPublicId is required")
    private String categoryPublicId;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "price must be >= 0.0")
    private BigDecimal price;

    /**
     * Optional display order.
     */
    private Integer displayOrder;

}
