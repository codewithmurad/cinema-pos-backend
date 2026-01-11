package com.telecamnig.cinemapos.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telecamnig.cinemapos.dto.DashboardPaymentSummaryDTO;
import com.telecamnig.cinemapos.dto.DashboardResponse;
import com.telecamnig.cinemapos.dto.DashboardResponseDTO;
import com.telecamnig.cinemapos.dto.DashboardSummaryDTO;
import com.telecamnig.cinemapos.dto.TicketDashboardSummaryDTO;
import com.telecamnig.cinemapos.dto.TicketPaymentSummaryDTO;
import com.telecamnig.cinemapos.dto.TopMovieDTO;
import com.telecamnig.cinemapos.dto.TopSellingItemDTO;
import com.telecamnig.cinemapos.entity.Show;
import com.telecamnig.cinemapos.repository.BookingRepository;
import com.telecamnig.cinemapos.repository.FoodOrderItemRepository;
import com.telecamnig.cinemapos.repository.FoodOrderRepository;
import com.telecamnig.cinemapos.repository.MovieRepository;
import com.telecamnig.cinemapos.repository.ScreenRepository;
import com.telecamnig.cinemapos.repository.ScreenSeatRepository;
import com.telecamnig.cinemapos.repository.ShowRepository;
import com.telecamnig.cinemapos.service.DashboardService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final ScreenSeatRepository screenSeatRepository;

    public DashboardServiceImpl(
            FoodOrderRepository foodOrderRepository,
            FoodOrderItemRepository foodOrderItemRepository,
            BookingRepository bookingRepository,
            ShowRepository showRepository,
            MovieRepository movieRepository,
            ScreenRepository screenRepository,
            ScreenSeatRepository screenSeatRepository) {
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
        this.bookingRepository = bookingRepository;
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.screenRepository = screenRepository;
        this.screenSeatRepository = screenSeatRepository;
    }

    // ==================== FOOD DASHBOARD METHODS ====================

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardResponse> getFoodDashboardData(
            LocalDate fromDate,
            LocalDate toDate,
            Integer limit) {
        
        try {
            log.info("Fetching FOOD dashboard data from {} to {}", fromDate, toDate);
            
            // Validate and adjust dates
            LocalDate[] dates = validateAndAdjustDates(fromDate, toDate);
            LocalDate actualFromDate = dates[0];
            LocalDate actualToDate = dates[1];
            
            // Set default limit
            int actualLimit = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);
            
            // Convert to LocalDateTime for queries
            LocalDateTime startDateTime = actualFromDate.atStartOfDay();
            LocalDateTime endDateTime = actualToDate.atTime(LocalTime.MAX);
            
            // Get summary data
            DashboardSummaryDTO summary = getFoodSummaryData(startDateTime, endDateTime);
            
            // Get payment breakdown
            DashboardPaymentSummaryDTO paymentSummary = getFoodPaymentSummaryData(startDateTime, endDateTime);
            
            // Get top selling items
            List<TopSellingItemDTO> topItems = getTopSellingFoodItems(startDateTime, endDateTime, actualLimit);
            
            // Build dashboard data
            DashboardResponseDTO dashboardData = DashboardResponseDTO.builder()
                    .summary(summary)
                    .paymentSummary(paymentSummary)
                    .topSellingItems(topItems)
                    .build();
            
            // Build response
            DashboardResponse response = DashboardResponse.builder()
                    .success(true)
                    .message("Food dashboard data retrieved successfully")
                    .data(dashboardData)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching food dashboard data", e);
            
            DashboardResponse errorResponse = DashboardResponse.builder()
                    .success(false)
                    .message("Failed to fetch food dashboard data: " + e.getMessage())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    // ==================== TICKET DASHBOARD METHODS ====================

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardResponse> getTicketDashboardData(
            LocalDate fromDate,
            LocalDate toDate,
            Integer limit) {
        
        try {
            log.info("Fetching TICKET dashboard data from {} to {}", fromDate, toDate);
            
            // Validate and adjust dates
            LocalDate[] dates = validateAndAdjustDates(fromDate, toDate);
            LocalDate actualFromDate = dates[0];
            LocalDate actualToDate = dates[1];
            
            // Set default limit
            int actualLimit = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);
            
            // Convert to LocalDateTime for queries
            LocalDateTime startDateTime = actualFromDate.atStartOfDay();
            LocalDateTime endDateTime = actualToDate.atTime(LocalTime.MAX);
            
            // Get summary data
            TicketDashboardSummaryDTO summary = getTicketSummaryData(startDateTime, endDateTime);
            
            // Get payment breakdown
            TicketPaymentSummaryDTO paymentSummary = getTicketPaymentSummaryData(startDateTime, endDateTime);
            
            // Get top performing movies
            List<TopMovieDTO> topMovies = getTopPerformingMovies(startDateTime, endDateTime, actualLimit);
            
            // Build dashboard data with ONLY ticket data
            DashboardResponseDTO dashboardData = DashboardResponseDTO.builder()
                    .summary(null)  // Food data
                    .paymentSummary(null)  // Food data
                    .topSellingItems(null)  // Food data
                    .ticketSummary(summary)
                    .ticketPaymentSummary(paymentSummary)
                    .topMovies(topMovies)
                    .build();
            
            // Build response
            DashboardResponse response = DashboardResponse.builder()
                    .success(true)
                    .message("Ticket dashboard data retrieved successfully")
                    .data(dashboardData)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching ticket dashboard data", e);
            
            DashboardResponse errorResponse = DashboardResponse.builder()
                    .success(false)
                    .message("Failed to fetch ticket dashboard data: " + e.getMessage())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    // ==================== PRIVATE HELPER METHODS (COMMON) ====================

    private LocalDate[] validateAndAdjustDates(LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now();
        
        if (fromDate == null) {
            fromDate = today;
        }
        
        if (toDate == null) {
            toDate = today;
        }
        
        // Ensure fromDate <= toDate
        if (fromDate.isAfter(toDate)) {
            LocalDate temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }
        
        return new LocalDate[]{fromDate, toDate};
    }

    // ==================== FOOD DASHBOARD HELPERS ====================

    private DashboardSummaryDTO getFoodSummaryData(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Integer paidStatus = 1; // FoodOrderStatus.PAID.getCode()
        
        List<Object[]> summaryResults = foodOrderRepository.getDashboardSummary(
                startDateTime, endDateTime, paidStatus
        );
        
        // Initialize with zeros
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal foodSales = BigDecimal.ZERO;
        BigDecimal vatCollected = BigDecimal.ZERO;
        Long totalOrders = 0L;
        
        // Check if we have results
        if (summaryResults != null && !summaryResults.isEmpty()) {
            Object[] result = summaryResults.get(0);
            
            // Safely cast the results
            if (result[0] != null) {
                totalRevenue = (BigDecimal) result[0];
            }
            if (result[1] != null) {
                foodSales = (BigDecimal) result[1];
            }
            if (result[2] != null) {
                vatCollected = (BigDecimal) result[2];
            }
            if (result[3] != null) {
                totalOrders = ((Number) result[3]).longValue();
            }
        }
        
        // Calculate average order value
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (totalOrders > 0) {
            averageOrderValue = totalRevenue.divide(
                    BigDecimal.valueOf(totalOrders), 
                    2, 
                    RoundingMode.HALF_UP
            );
        }
        
        return DashboardSummaryDTO.builder()
                .totalRevenue(totalRevenue)
                .foodSales(foodSales)
                .vatCollected(vatCollected)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .build();
    }

    private DashboardPaymentSummaryDTO getFoodPaymentSummaryData(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Integer paidStatus = 1; // FoodOrderStatus.PAID.getCode()
        
        List<Object[]> results = foodOrderRepository.getPaymentMethodBreakdown(
                startDateTime, endDateTime, paidStatus
        );
        
        // Initialize with zeros
        BigDecimal cashFoodSales = BigDecimal.ZERO;
        BigDecimal cashVatCollected = BigDecimal.ZERO;
        BigDecimal cashTotalRevenue = BigDecimal.ZERO;
        long cashOrderCount = 0;
        
        BigDecimal posFoodSales = BigDecimal.ZERO;
        BigDecimal posVatCollected = BigDecimal.ZERO;
        BigDecimal posTotalRevenue = BigDecimal.ZERO;
        long posOrderCount = 0;
        
        // Parse results
        if (results != null) {
            for (Object[] row : results) {
                String paymentMode = (String) row[0];
                BigDecimal foodSales = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                BigDecimal vatCollected = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                BigDecimal totalRevenue = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
                Long orderCount = row[4] != null ? ((Number) row[4]).longValue() : 0L;
                
                if ("CASH".equalsIgnoreCase(paymentMode)) {
                    cashFoodSales = foodSales;
                    cashVatCollected = vatCollected;
                    cashTotalRevenue = totalRevenue;
                    cashOrderCount = orderCount;
                } else if ("POS".equalsIgnoreCase(paymentMode)) {
                    posFoodSales = foodSales;
                    posVatCollected = vatCollected;
                    posTotalRevenue = totalRevenue;
                    posOrderCount = orderCount;
                }
            }
        }
        
        return DashboardPaymentSummaryDTO.builder()
                .cashFoodSales(cashFoodSales)
                .cashVatCollected(cashVatCollected)
                .cashTotalRevenue(cashTotalRevenue)
                .cashOrderCount(cashOrderCount)
                .posFoodSales(posFoodSales)
                .posVatCollected(posVatCollected)
                .posTotalRevenue(posTotalRevenue)
                .posOrderCount(posOrderCount)
                .build();
    }
    
    private List<TopSellingItemDTO> getTopSellingFoodItems(
            LocalDateTime startDateTime, 
            LocalDateTime endDateTime, 
            int limit) {
        
        Integer paidStatus = 1;
        
        List<Object[]> results = foodOrderItemRepository.getTopSellingItems(
                startDateTime, endDateTime, paidStatus
        );
        
        List<TopSellingItemDTO> topItems = new ArrayList<>();
        
        if (results != null) {
            int count = 0;
            for (Object[] row : results) {
                if (count >= limit) break;
                
                String foodName = (String) row[0];
                String categoryName = (String) row[1];
                Number totalQuantity = (Number) row[2];
                BigDecimal totalAmount = (BigDecimal) row[3];
                
                TopSellingItemDTO item = TopSellingItemDTO.builder()
                        .foodName(foodName != null ? foodName : "Unknown")
                        .categoryName(categoryName != null ? categoryName : "Uncategorized")
                        .totalQuantity(totalQuantity != null ? totalQuantity.longValue() : 0L)
                        .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                        .build();
                
                topItems.add(item);
                count++;
            }
        }
        
        return topItems;
    }

 // ==================== TICKET DASHBOARD HELPERS ====================

    private TicketDashboardSummaryDTO getTicketSummaryData(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        String issuedStatus = "ISSUED";
        
        // Get ticket summary from booking repository
        List<Object[]> bookingSummary = bookingRepository.getTicketDashboardSummary(
                startDateTime, endDateTime, issuedStatus
        );
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Long ticketsSold = 0L;
        BigDecimal vatCollected = BigDecimal.ZERO;
        
        if (bookingSummary != null && !bookingSummary.isEmpty()) {
            Object[] result = bookingSummary.get(0);
            
            if (result[0] != null) totalRevenue = (BigDecimal) result[0];
            if (result[1] != null) ticketsSold = ((Number) result[1]).longValue();
            if (result[2] != null) vatCollected = (BigDecimal) result[2];
        }
        
        // Calculate average ticket price
        BigDecimal avgTicketPrice = BigDecimal.ZERO;
        if (ticketsSold > 0) {
            avgTicketPrice = totalRevenue.divide(
                    BigDecimal.valueOf(ticketsSold), 
                    2, 
                    RoundingMode.HALF_UP
            );
        }
        
        // Calculate occupancy rate
        BigDecimal occupancyRate = calculateOccupancyRate(startDateTime, endDateTime);
        
        return TicketDashboardSummaryDTO.builder()
                .totalRevenue(totalRevenue)
                .ticketsSold(ticketsSold)
                .vatCollected(vatCollected)
                .avgTicketPrice(avgTicketPrice)
                .occupancyRate(occupancyRate)
                .build();
    }

    private TicketPaymentSummaryDTO getTicketPaymentSummaryData(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        String issuedStatus = "ISSUED";
        
        List<Object[]> paymentResults = bookingRepository.getTicketPaymentSummary(
                startDateTime, endDateTime, issuedStatus
        );
        
        // Initialize with zeros
        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal cashVat = BigDecimal.ZERO;
        Long cashTickets = 0L;
        
        BigDecimal posRevenue = BigDecimal.ZERO;
        BigDecimal posVat = BigDecimal.ZERO;
        Long posTickets = 0L;
        
        BigDecimal otherRevenue = BigDecimal.ZERO;
        BigDecimal otherVat = BigDecimal.ZERO;
        Long otherTickets = 0L;
        
        if (paymentResults != null) {
            for (Object[] row : paymentResults) {
                String paymentMode = (String) row[0];
                BigDecimal revenue = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                BigDecimal vat = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                Long ticketCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                
                // Only handle CASH and POS - ignore anything else
                if ("CASH".equalsIgnoreCase(paymentMode)) {
                    cashRevenue = revenue;
                    cashVat = vat;
                    cashTickets = ticketCount;
                } else if ("POS".equalsIgnoreCase(paymentMode)) {
                    posRevenue = revenue;
                    posVat = vat;
                    posTickets = ticketCount;
                }
                // No else - we ignore other payment modes
            }
        }
        
        return TicketPaymentSummaryDTO.builder()
                .cashRevenue(cashRevenue)
                .cashVat(cashVat)
                .cashTicketCount(cashTickets)
                .posRevenue(posRevenue)
                .posVat(posVat)
                .posTicketCount(posTickets)
                .build();
    }

    private List<TopMovieDTO> getTopPerformingMovies(
            LocalDateTime startDateTime, 
            LocalDateTime endDateTime, 
            int limit) {
        
        String issuedStatus = "ISSUED";
        
        Pageable pageable = PageRequest.of(0, Math.min(limit, 100)); // Max 100
        List<Object[]> allMovies = bookingRepository.findTopPerformingMovies(
                startDateTime, endDateTime, issuedStatus, pageable
        );
        
        List<TopMovieDTO> topMovies = new ArrayList<>();
        
        // Apply limit manually
        int maxSize = Math.min(limit, allMovies.size());
        for (int i = 0; i < maxSize; i++) {
            Object[] row = allMovies.get(i);
            String movieTitle = (String) row[0];
            Long ticketsSold = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            BigDecimal totalRevenue = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            
            TopMovieDTO movie = TopMovieDTO.builder()
                    .movieTitle(movieTitle != null ? movieTitle : "Unknown Movie")
                    .ticketsSold(ticketsSold)
                    .totalRevenue(totalRevenue)
                    .build();
            
            topMovies.add(movie);
        }
        
        return topMovies;
    }

    private BigDecimal calculateOccupancyRate(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        try {
            // 1. Get tickets sold count from existing query
            String issuedStatus = "ISSUED";
            List<Object[]> summary = bookingRepository.getTicketDashboardSummary(
                startDateTime, endDateTime, issuedStatus
            );
            
            Long ticketsSold = 0L;
            if (summary != null && !summary.isEmpty()) {
                Object[] result = summary.get(0);
                if (result[1] != null) {
                    ticketsSold = ((Number) result[1]).longValue();
                }
            }
            
            // 2. If no tickets sold, return 0%
            if (ticketsSold == 0) {
                return BigDecimal.ZERO;
            }
            
            // 3. Get shows in date range
            List<Show> shows = showRepository.findShowsByDateRange(startDateTime, endDateTime);
            
            // 4. Calculate total seat capacity
            long totalSeats = 0;
            for (Show show : shows) {
                Long screenId = show.getScreenId();
                if (screenId != null) {
                    Long screenCapacity = screenSeatRepository.countActiveSeatsByScreenId(screenId);
                    totalSeats += (screenCapacity != null ? screenCapacity : 0);
                }
            }
            
            // 5. Calculate occupancy
            if (totalSeats > 0) {
                double occupancy = (ticketsSold * 100.0) / totalSeats;
                return BigDecimal.valueOf(occupancy)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            
            return BigDecimal.ZERO;
            
        } catch (Exception e) {
            log.error("Error calculating occupancy rate", e);
            return BigDecimal.ZERO;
        }
    }
    
}