package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FoodOrderDetailResponse {

    private String orderPublicId;
    private String billNo;
    private String paymentMode;

    private BigDecimal subTotal;
    private BigDecimal vatPercentage;
    private BigDecimal vatAmount;
    private BigDecimal grandTotal;

    private Integer totalItems;
    private Integer status;
    private Integer printCount;

    private LocalDateTime createdAt;

    private List<FoodOrderItemResponse> items;
    
}
