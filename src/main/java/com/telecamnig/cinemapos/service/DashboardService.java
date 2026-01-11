package com.telecamnig.cinemapos.service;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;

import com.telecamnig.cinemapos.dto.DashboardResponse;

/**
 * Dashboard Service for providing analytics and reports.
 */
public interface DashboardService {
    
    /**
     * Get comprehensive FOOD dashboard data for a date range.
     * Defaults to today if no dates provided.
     * Only includes PAID orders (status = 1).
     * 
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @param limit Top items limit
     * @return DashboardResponse with all metrics
     */
    ResponseEntity<DashboardResponse> getFoodDashboardData(
            LocalDate fromDate,
            LocalDate toDate,
            Integer limit
    );
    
    /**
     * Get comprehensive TICKET dashboard data for a date range.
     * Defaults to today if no dates provided.
     * Only includes ISSUED bookings (status = ISSUED).
     * 
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @param limit Top movies limit
     * @return DashboardResponse with all metrics
     */
    ResponseEntity<DashboardResponse> getTicketDashboardData(
            LocalDate fromDate,
            LocalDate toDate,
            Integer limit
    );
    
}