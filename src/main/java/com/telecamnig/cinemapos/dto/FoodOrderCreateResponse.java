package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrderCreateResponse extends CommonApiResponse {
    
    private String billNo;
    
    private String orderPublicId;
    
    public FoodOrderCreateResponse(boolean success, String message) {
        super(success, message);
    }
    
    public FoodOrderCreateResponse(boolean success, String message, String billNo, String orderPublicId) {
        super(success, message);
        this.billNo = billNo;
        this.orderPublicId = orderPublicId;
    }
    
}