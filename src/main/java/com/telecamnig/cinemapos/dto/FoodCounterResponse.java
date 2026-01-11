package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;

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
public class FoodCounterResponse {

    private String publicId;
    
    private String name;
    
    private BigDecimal price;
    
    private String imagePath;
    
    private Integer displayOrder;

    private String categoryPublicId;

}
