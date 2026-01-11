package com.telecamnig.cinemapos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for updating Food Category.
 * Status is system-controlled and NOT part of this request.
 */
@Getter
@Setter
public class UpdateFoodCategoryRequest {

    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;

    @Min(value = 0, message = "displayOrder must be >= 0")
    @Max(value = 9999, message = "displayOrder too large")
    private Integer displayOrder;

}
