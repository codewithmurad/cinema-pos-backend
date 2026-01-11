package com.telecamnig.cinemapos.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.CreateFoodOrderRequest;
import com.telecamnig.cinemapos.dto.FoodItemRequest;
import com.telecamnig.cinemapos.dto.FoodOrderAdminResponse;
import com.telecamnig.cinemapos.dto.FoodOrderCreateResponse;
import com.telecamnig.cinemapos.dto.FoodOrderDetailResponse;
import com.telecamnig.cinemapos.dto.FoodOrderItemResponse;
import com.telecamnig.cinemapos.entity.Food;
import com.telecamnig.cinemapos.entity.FoodOrder;
import com.telecamnig.cinemapos.entity.FoodOrderItem;
import com.telecamnig.cinemapos.repository.FoodOrderItemRepository;
import com.telecamnig.cinemapos.repository.FoodOrderRepository;
import com.telecamnig.cinemapos.repository.FoodRepository;
import com.telecamnig.cinemapos.service.FoodOrderService;
import com.telecamnig.cinemapos.utility.Constants.FoodOrderStatus;
import com.telecamnig.cinemapos.utility.Constants.FoodStatus;
import com.telecamnig.cinemapos.utility.Constants.PaymentMode;


@Service
public class FoodOrderServiceImpl implements FoodOrderService {

    private static final Logger log = LoggerFactory.getLogger(FoodOrderServiceImpl.class);

    private final FoodRepository foodRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;

    @Value("${cinema.food.vat.percentage}")
    private BigDecimal foodVatPercentage;

    public FoodOrderServiceImpl(
            FoodRepository foodRepository,
            FoodOrderRepository foodOrderRepository,
            FoodOrderItemRepository foodOrderItemRepository) {

        this.foodRepository = foodRepository;
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
    }

    @Override
    @Transactional
    public ResponseEntity<FoodOrderCreateResponse> createFoodOrder(CreateFoodOrderRequest request) {

        log.info("Creating food order");

        // ==================== BASIC VALIDATION ====================
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(FoodOrderCreateResponse.builder()
                            .success(false)
                            .message("At least one food item is required")
                            .build());
        }

        // ==================== VALIDATE PAYMENT MODE ====================
        if (request.getPaymentMode() == null || request.getPaymentMode().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(FoodOrderCreateResponse.builder()
                            .success(false)
                            .message("Payment mode is required")
                            .build());
        }

        PaymentMode paymentMode;
        try {
            paymentMode = PaymentMode.valueOf(request.getPaymentMode().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(FoodOrderCreateResponse.builder()
                            .success(false)
                            .message("Invalid payment mode")
                            .build());
        }

        // ==================== DUPLICATE FOOD CHECK ====================
        List<String> foodPublicIds = request.getItems()
                .stream()
                .map(FoodItemRequest::getFoodPublicId)
                .toList();

        Set<String> uniqueFoodIds = new HashSet<>(foodPublicIds);
        if (uniqueFoodIds.size() != foodPublicIds.size()) {
            return ResponseEntity.badRequest()
                    .body(FoodOrderCreateResponse.builder()
                            .success(false)
                            .message("Duplicate food items are not allowed")
                            .build());
        }

        // ==================== FETCH ACTIVE FOODS (DB-LEVEL SAFETY) ====================
        List<Food> foods = foodRepository.findByPublicIdInAndStatus(
                foodPublicIds,
                FoodStatus.ACTIVE.getCode()
        );

        if (foods.size() != foodPublicIds.size()) {
            return ResponseEntity.badRequest()
                    .body(FoodOrderCreateResponse.builder()
                            .success(false)
                            .message("Invalid or inactive food selection")
                            .build());
        }

        Map<String, Food> foodMap = foods.stream()
                .collect(Collectors.toMap(Food::getPublicId, f -> f));

        // ==================== CALCULATE SUBTOTAL ====================
        BigDecimal subTotal = BigDecimal.ZERO;

        for (FoodItemRequest item : request.getItems()) {
            Food food = foodMap.get(item.getFoodPublicId());

            BigDecimal lineTotal = food.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            subTotal = subTotal.add(lineTotal);
        }

        // ==================== VAT CALCULATION ====================
        BigDecimal vatAmount = subTotal
                .multiply(foodVatPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal grandTotal = subTotal.add(vatAmount);

        int totalItems = request.getItems()
                .stream()
                .mapToInt(FoodItemRequest::getQuantity)
                .sum();

        
        // ==================== CREATE FOOD ORDER ====================
        FoodOrder order = new FoodOrder();
        order.setBillNo(generateBillNo());
        order.setSubTotal(subTotal);
        order.setVatPercentage(foodVatPercentage);
        order.setVatAmount(vatAmount);
        order.setGrandTotal(grandTotal);
        order.setTotalItems(totalItems);
        order.setPaymentMode(paymentMode.getLabel());
        order.setStatus(FoodOrderStatus.PAID.getCode());

        FoodOrder savedOrder = foodOrderRepository.save(order);

        // ==================== CREATE ORDER ITEMS ====================
        List<FoodOrderItem> orderItems = new ArrayList<>();

        for (FoodItemRequest item : request.getItems()) {

            Food food = foodMap.get(item.getFoodPublicId());

            FoodOrderItem orderItem = new FoodOrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setFoodId(food.getId());
            orderItem.setFoodPublicId(food.getPublicId());
            orderItem.setFoodName(food.getName());
            orderItem.setUnitPrice(food.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setLineTotal(
                    food.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            );

            orderItems.add(orderItem);
        }

        foodOrderItemRepository.saveAll(orderItems);

        log.info("Food order created successfully. BillNo={}, PublicId={}", 
                savedOrder.getBillNo(), savedOrder.getPublicId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FoodOrderCreateResponse.builder()
                        .success(true)
                        .message("Food bill created successfully")
                        .billNo(savedOrder.getBillNo())
                        .orderPublicId(savedOrder.getPublicId())
                        .build());
    }
    
    @Override
    @Transactional
    public ResponseEntity<CommonApiResponse> incrementPrintCount(String orderPublicId) {

        try {
            // ==================== VALIDATION ====================
            if (!StringUtils.hasText(orderPublicId)) {
                return ResponseEntity.badRequest()
                        .body(new CommonApiResponse(false, "Invalid order id"));
            }

            // ==================== FETCH ORDER ====================
            FoodOrder order = foodOrderRepository.findByPublicId(orderPublicId.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Food order not found"));

            // ==================== INCREMENT PRINT COUNT ====================
            order.incrementPrintCount();
            foodOrderRepository.save(order);

            log.debug("Food receipt print count updated: order={}, count={}",
                    orderPublicId, order.getPrintCount());

            return ResponseEntity.ok(
                    new CommonApiResponse(true, "Food receipt print count updated")
            );

        } catch (IllegalArgumentException ex) {
            log.warn("Food order not found: {}", orderPublicId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse(false, ex.getMessage()));

        } catch (Exception ex) {
            log.error("Failed to increment food receipt print count", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse(false, "Failed to update print count"));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Page<FoodOrderAdminResponse>> getFoodOrdersByDate(
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Integer status,
            Pageable pageable) {

        log.info("Fetching food orders from {} to {}, status={}",
                fromDate, toDate, status);

        Page<FoodOrder> orders =
                foodOrderRepository.findFoodOrdersByDateRange(
                        fromDate, toDate, status, pageable
                );

        Page<FoodOrderAdminResponse> response =
                orders.map(order -> FoodOrderAdminResponse.builder()
                        .orderPublicId(order.getPublicId())
                        .subTotal(order.getSubTotal())
                        .vatAmount(order.getVatAmount())
                        .totalAmount(order.getGrandTotal())
                        .itemCount(order.getTotalItems())
                        .status(order.getStatus())
                        .printCount(order.getPrintCount())
                        .createdAt(order.getCreatedAt())
                        .build()
                );

        return ResponseEntity.ok(response);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<FoodOrderDetailResponse> getFoodOrderDetails(String orderPublicId) {

        if (!StringUtils.hasText(orderPublicId)) {
            return ResponseEntity.badRequest().build();
        }

        // ==================== FETCH ORDER ====================
        FoodOrder order = foodOrderRepository.findByPublicId(orderPublicId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Food order not found"));

        // ==================== FETCH ITEMS ====================
        List<FoodOrderItem> items =
                foodOrderItemRepository.findByOrderId(order.getId());

        // ==================== MAP ITEMS ====================
        List<FoodOrderItemResponse> itemResponses = items.stream()
                .map(item -> FoodOrderItemResponse.builder()
                        .foodName(item.getFoodName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        // ==================== BUILD RESPONSE ====================
        FoodOrderDetailResponse response = FoodOrderDetailResponse.builder()
                .orderPublicId(order.getPublicId())
                .billNo(order.getBillNo())
                .paymentMode(order.getPaymentMode())
                .subTotal(order.getSubTotal())
                .vatPercentage(order.getVatPercentage())
                .vatAmount(order.getVatAmount())
                .grandTotal(order.getGrandTotal())
                .totalItems(order.getTotalItems())
                .status(order.getStatus())
                .printCount(order.getPrintCount())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();

        return ResponseEntity.ok(response);
    }


    // ==================== UTIL ====================

    private String generateBillNo() {
        return "F-" + LocalDate.now() + "-" + System.currentTimeMillis();
    }

}
