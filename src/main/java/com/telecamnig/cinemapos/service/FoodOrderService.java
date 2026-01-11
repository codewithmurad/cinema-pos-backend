package com.telecamnig.cinemapos.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.CreateFoodOrderRequest;
import com.telecamnig.cinemapos.dto.FoodOrderAdminResponse;
import com.telecamnig.cinemapos.dto.FoodOrderCreateResponse;
import com.telecamnig.cinemapos.dto.FoodOrderDetailResponse;

public interface FoodOrderService {

    ResponseEntity<FoodOrderCreateResponse> createFoodOrder(CreateFoodOrderRequest request);
    
    ResponseEntity<CommonApiResponse> incrementPrintCount(String orderPublicId);
    
    ResponseEntity<Page<FoodOrderAdminResponse>> getFoodOrdersByDate(
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Integer status,
            Pageable pageable
    );
    
    ResponseEntity<FoodOrderDetailResponse> getFoodOrderDetails(String orderPublicId);
    
}
