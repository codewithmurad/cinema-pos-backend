package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FoodOrderItemResponse {

    private String foodName;
    
    private BigDecimal unitPrice;
    
    private Integer quantity;

    private BigDecimal lineTotal;

}
