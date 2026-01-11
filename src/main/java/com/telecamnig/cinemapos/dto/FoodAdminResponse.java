package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for Admin Food listing.
 */
@Data
@Builder
public class FoodAdminResponse {

    private String publicId;
    private String name;
    private BigDecimal price;
    private String imagePath;

    private String categoryPublicId;
    private Integer status;

    private Integer displayOrder;
    private LocalDateTime updatedAt;

}
