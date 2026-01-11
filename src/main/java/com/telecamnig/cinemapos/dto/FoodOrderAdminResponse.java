package com.telecamnig.cinemapos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FoodOrderAdminResponse {

    private String orderPublicId;
    private BigDecimal subTotal;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private Integer status;
    private Integer printCount;
    private LocalDateTime createdAt;

}
