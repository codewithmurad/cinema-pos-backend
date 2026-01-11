package com.telecamnig.cinemapos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for adding a Food Category.
 * Status is system-managed (default ACTIVE).
 */
@Getter
@Setter
public class AddFoodCategoryRequest {

    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;

    /**
     * Optional display order for Food Counter UI.
     */
    @Min(value = 0, message = "displayOrder must be >= 0")
    @Max(value = 9999, message = "displayOrder too large")
    private Integer displayOrder;

}
