package com.telecamnig.cinemapos.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.CreateFoodOrderRequest;
import com.telecamnig.cinemapos.dto.FoodOrderAdminResponse;
import com.telecamnig.cinemapos.dto.FoodOrderCreateResponse;
import com.telecamnig.cinemapos.dto.FoodOrderDetailResponse;
import com.telecamnig.cinemapos.service.FoodOrderService;

import jakarta.validation.Valid;

/**
 * FoodOrderController
 *
 * Handles food billing operations for POS.
 */
@RestController
@RequestMapping("/api/v1/food-order")
public class FoodOrderController {

    private final FoodOrderService foodOrderService;

    public FoodOrderController(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    /**
     * Create a new food bill.
     *
     * POST /api/v1/food-order/bill
     */
    @PostMapping("/bill")
    public ResponseEntity<FoodOrderCreateResponse> createFoodOrder(
            @Valid @RequestBody CreateFoodOrderRequest request) {

        return foodOrderService.createFoodOrder(request);
    }
    
    /**
     * Increment print count for food receipt.
     *
     * Purpose:
     * - Track how many times a food receipt is printed
     * - Used for reprints and audit trail
     *
     * IMPORTANT:
     * - This API DOES NOT print
     * - Frontend handles actual printing
     */
    @PutMapping("/{orderPublicId}/print")
    public ResponseEntity<CommonApiResponse> incrementFoodReceiptPrintCount(
            @PathVariable String orderPublicId) {

        return foodOrderService.incrementPrintCount(orderPublicId);
    }
    
    /**
     * Fetch food bills (orders) by date range.
     *
     * Used for:
     * - Admin listing
     * - Cash reconciliation
     * - Dashboard & reports
     *
     * Default sort: createdAt DESC (latest first)
     */
    @GetMapping("/by-date")
    public ResponseEntity<Page<FoodOrderAdminResponse>> getFoodOrdersByDate(
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) Integer status,
            Pageable pageable) {

        return foodOrderService.getFoodOrdersByDate(
                fromDate, toDate, status, pageable
        );
    }
    
    /**
     * Fetch complete food order details for history view.
     * Used when admin clicks "View" icon.
     */
    @GetMapping("/{orderPublicId}/details")
    public ResponseEntity<FoodOrderDetailResponse> getFoodOrderDetails(
            @PathVariable String orderPublicId) {

        return foodOrderService.getFoodOrderDetails(orderPublicId);
    }

}
