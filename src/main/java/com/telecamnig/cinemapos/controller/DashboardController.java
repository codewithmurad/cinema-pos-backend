package com.telecamnig.cinemapos.controller;

import com.telecamnig.cinemapos.dto.DashboardResponse;
import com.telecamnig.cinemapos.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Dashboard Controller
 * 
 * Provides analytics and reporting APIs for food AND ticket sales dashboard.
 * All data is filtered by date range and includes only PAID/ISSUED orders.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Food and Ticket sales analytics and reporting APIs")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Get comprehensive FOOD dashboard data
     * 
     * Returns all food dashboard metrics in a single call:
     * - Summary (total revenue, food sales, VAT, orders, average order value)
     * - Payment method breakdown (Cash vs POS)
     * - Top selling food items
     * 
     * Defaults to today's data if no dates provided.
     * Only includes PAID orders (status = 1).
     */
    @GetMapping("/food")
    @Operation(
        summary = "Get comprehensive FOOD dashboard data",
        description = """
            Returns all FOOD dashboard metrics including:
            1. Summary: Total Revenue, Food Sales, VAT Collected, Total Orders, Average Order Value
            2. Payment Breakdown: Cash vs POS with food sales and VAT
            3. Top Selling Items: Top food items by quantity sold
            
            Defaults to today's data if no dates provided.
            Only includes PAID orders (status = 1).
            """
    )
    public ResponseEntity<DashboardResponse> getFoodDashboardData(
            @Parameter(description = "Start date (yyyy-MM-dd), inclusive. Default: today", example = "2024-01-10")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate fromDate,
            
            @Parameter(description = "End date (yyyy-MM-dd), inclusive. Default: today", example = "2024-01-11")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate toDate,
            
            @Parameter(description = "Number of top items to return (default: 5, max: 20)", example = "5")
            @RequestParam(required = false, defaultValue = "5") 
            Integer limit) {
        
        log.info("FOOD Dashboard API called - From: {}, To: {}, Limit: {}", fromDate, toDate, limit);
        return dashboardService.getFoodDashboardData(fromDate, toDate, limit);
    }

    /**
     * Get today's FOOD dashboard data
     * 
     * Quick endpoint for getting today's food dashboard data.
     */
    @GetMapping("/food/today")
    @Operation(summary = "Get today's FOOD dashboard data")
    public ResponseEntity<DashboardResponse> getTodayFoodDashboardData(
            @Parameter(description = "Number of top items to return (default: 5)", example = "5")
            @RequestParam(required = false, defaultValue = "5") Integer limit) {
        
        log.info("Today's FOOD dashboard API called - Limit: {}", limit);
        LocalDate today = LocalDate.now();
        return dashboardService.getFoodDashboardData(today, today, limit);
    }

    /**
     * Get comprehensive TICKET dashboard data
     * 
     * Returns all ticket dashboard metrics in a single call:
     * - Summary (total revenue, tickets sold, VAT, avg ticket price, occupancy rate)
     * - Payment method breakdown (Cash vs POS vs Other)
     * - Top performing movies
     * 
     * Defaults to today's data if no dates provided.
     * Only includes ISSUED bookings (status = ISSUED).
     */
    @GetMapping("/ticket")
    @Operation(
        summary = "Get comprehensive TICKET dashboard data",
        description = """
            Returns all TICKET dashboard metrics including:
            1. Summary: Total Revenue, Tickets Sold, VAT Collected, Avg Ticket Price, Occupancy Rate
            2. Payment Breakdown: Cash vs POS vs Other with revenue and VAT
            3. Top Performing Movies: Top movies by tickets sold and revenue
            
            Defaults to today's data if no dates provided.
            Only includes ISSUED bookings (status = ISSUED).
            """
    )
    public ResponseEntity<DashboardResponse> getTicketDashboardData(
            @Parameter(description = "Start date (yyyy-MM-dd), inclusive. Default: today", example = "2024-01-10")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate fromDate,
            
            @Parameter(description = "End date (yyyy-MM-dd), inclusive. Default: today", example = "2024-01-11")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate toDate,
            
            @Parameter(description = "Number of top movies to return (default: 5, max: 20)", example = "5")
            @RequestParam(required = false, defaultValue = "5") 
            Integer limit) {
        
        log.info("TICKET Dashboard API called - From: {}, To: {}, Limit: {}", fromDate, toDate, limit);
        return dashboardService.getTicketDashboardData(fromDate, toDate, limit);
    }

    /**
     * Get today's TICKET dashboard data
     * 
     * Quick endpoint for getting today's ticket dashboard data.
     */
    @GetMapping("/ticket/today")
    @Operation(summary = "Get today's TICKET dashboard data")
    public ResponseEntity<DashboardResponse> getTodayTicketDashboardData(
            @Parameter(description = "Number of top movies to return (default: 5)", example = "5")
            @RequestParam(required = false, defaultValue = "5") Integer limit) {
        
        log.info("Today's TICKET dashboard API called - Limit: {}", limit);
        LocalDate today = LocalDate.now();
        return dashboardService.getTicketDashboardData(today, today, limit);
    }

}