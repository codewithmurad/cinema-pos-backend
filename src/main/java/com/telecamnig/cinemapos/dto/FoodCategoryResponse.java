package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used by Food Counter UI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodCategoryResponse {

	private String publicId;

	private String name;

	private String imagePath;

	private Integer displayOrder;

}
