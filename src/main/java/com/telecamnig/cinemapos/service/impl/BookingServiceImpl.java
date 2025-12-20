package com.telecamnig.cinemapos.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.telecamnig.cinemapos.dto.BookingConfirmationEvent;
import com.telecamnig.cinemapos.dto.BookingDetailResponse;
import com.telecamnig.cinemapos.dto.BookingListResponse;
import com.telecamnig.cinemapos.dto.BookingRequestDTO;
import com.telecamnig.cinemapos.dto.BookingResponseDTO;
import com.telecamnig.cinemapos.dto.BookingStatisticsResponse;
import com.telecamnig.cinemapos.dto.PaginationInfo;
import com.telecamnig.cinemapos.dto.PaymentMethodStats;
import com.telecamnig.cinemapos.dto.SeatStateEvent;
import com.telecamnig.cinemapos.entity.Booking;
import com.telecamnig.cinemapos.entity.Show;
import com.telecamnig.cinemapos.entity.ShowSeat;
import com.telecamnig.cinemapos.entity.User;
import com.telecamnig.cinemapos.repository.BookingRepository;
import com.telecamnig.cinemapos.repository.ShowRepository;
import com.telecamnig.cinemapos.repository.ShowSeatRepository;
import com.telecamnig.cinemapos.repository.UserRepository;
import com.telecamnig.cinemapos.service.BookingService;
import com.telecamnig.cinemapos.service.SeatHoldService;
import com.telecamnig.cinemapos.utility.ApiResponseMessage;
import com.telecamnig.cinemapos.utility.Constants.BookingStatus;
import com.telecamnig.cinemapos.utility.Constants.PaymentMode;
import com.telecamnig.cinemapos.utility.Constants.ShowSeatState;
import com.telecamnig.cinemapos.utility.Constants.ShowStatus;
import com.telecamnig.cinemapos.websocket.WebSocketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of BookingService for managing cinema bookings.
 * 
 * FEATURES:
 * - Seat validation and availability checks
 * - Payment processing (Cash/Online)
 * - Real-time synchronization across 4 POS systems
 * - Ticket generation and printing
 * - Booking cancellation and refunds
 * 
 * BUSINESS RULES:
 * - Seats must be HELD before booking confirmation
 * - Seat must be held by the SAME counter user who confirms booking
 * - 5-minute hold timeout for seat selection
 * - Immutable booking records for audit trail
 * - Real-time updates via WebSocket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;
    private final SeatHoldService seatHoldService;
    private final WebSocketService webSocketService;
    
    public static final int MAX_SEATS_PER_BOOKING = 10; // Maximum seats per booking
    public static final int MIN_MINUTES_BEFORE_SHOW_FOR_BOOKING = 0; // No bookings if show started

    @Value("${cinema.vat.percentage}")
    private BigDecimal vatPercentage;
    
    // ========== MAIN BOOKING METHODS ==========

    @Override
    @Transactional
    public ResponseEntity<BookingDetailResponse> createBooking(BookingRequestDTO bookingRequest) {
        try {
            log.info("Creating booking for show: {}, seats: {}", 
                    bookingRequest.getShowPublicId(), bookingRequest.getSeatPublicIds());

            // 1. Validate booking request body
            ResponseEntity<BookingDetailResponse> validationResponse = validateBookingRequest(bookingRequest);
            if (validationResponse != null) {
                return validationResponse;
            }

            // Optional: enforce max seats per booking
            if (bookingRequest.getSeatPublicIds().size() > MAX_SEATS_PER_BOOKING) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST,
                        "Cannot book more than " + MAX_SEATS_PER_BOOKING + " seats in a single booking");
            }

            // 2. Get current authenticated user (counter staff)
            User currentUser = getCurrentUser(bookingRequest.getBookedByUserEmail());
            if (currentUser == null) {
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found");
            }

            // This identifier MUST match what you use as `reservedBy`
//            String bookingUserIdentifier = null;
//            if (SecurityContextHolder.getContext().getAuthentication() != null) {
//                bookingUserIdentifier = SecurityContextHolder.getContext().getAuthentication().getName();
//            }

            // 3. Retrieve show details
            Show show = showRepository.findByPublicId(bookingRequest.getShowPublicId())
                    .orElseThrow(() -> new RuntimeException(ApiResponseMessage.SHOW_NOT_FOUND));

            // 4. Validate show is active and not in past
            if (!isShowActiveAndFuture(show)) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Cannot book for inactive or past show");
            }

            // 5. Validate and retrieve show seats
            List<ShowSeat> showSeats = validateAndGetShowSeats(bookingRequest.getSeatPublicIds(), show.getId());
            
            // 6. Check seat availability and holds
            //    BUSINESS RULE:
            //    - Seat MUST be in HELD state
            //    - Seat MUST be held by the SAME user who is doing this booking
            //    - Seat hold MUST NOT be expired
            if (!areSeatsAvailableForBooking(showSeats, currentUser.getEmailId())) {
                return buildErrorResponse(HttpStatus.CONFLICT,
                        "One or more seats are not available for booking or not held by this user");
            }

            // 7. FINAL SAFETY CHECK (REPLACES DB UNIQUE CONSTRAINT)
            for (ShowSeat showSeat : showSeats) {
                boolean alreadyIssued =
                    bookingRepository.existsByShowPublicIdAndSeatPublicIdAndStatus(
                        bookingRequest.getShowPublicId(),
                        showSeat.getSeatPublicId(),
                        BookingStatus.ISSUED.getLabel()
                    );

                if (alreadyIssued) {
                    return buildErrorResponse(
                        HttpStatus.CONFLICT,
                        "Seat already booked: " + showSeat.getSeatLabel()
                    );
                }
            }
  
            // 8. Create booking records and update database
            List<Booking> bookings = createBookingRecords(
                    bookingRequest, show, showSeats, currentUser.getId()
            );


            // 9. Update show seat states to SOLD and confirm holds
            updateShowSeatStates(showSeats, bookings.get(0));

            // 10. Broadcast booking confirmation to all POS systems
            broadcastBookingConfirmation(bookings, showSeats);

            // 11. Prepare and return success response
            BookingResponseDTO bookingResponse = convertToBookingResponseDTO(bookings.get(0), showSeats);

            log.info("Booking created successfully - Booking: {}, Show: {}, Seats: {}", 
                    bookings.get(0).getPublicId(), show.getPublicId(), 
                    showSeats.stream().map(ShowSeat::getSeatLabel).collect(Collectors.toList()));

            return ResponseEntity.ok(BookingDetailResponse.builder()
                    .success(true)
                    .message("Booking confirmed successfully")
                    .booking(bookingResponse)
                    .build());

        } catch (Exception e) {
            log.error("Error creating booking for show: {}", bookingRequest.getShowPublicId(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to create booking: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<BookingDetailResponse> getBookingByPublicId(String bookingPublicId) {
        try {
            // Validate booking public ID
            if (!StringUtils.hasText(bookingPublicId)) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiResponseMessage.INVALID_INPUT);
            }

            // Retrieve booking from repository
            Booking booking = bookingRepository.findByPublicId(bookingPublicId.trim())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Get associated show seats for complete booking details
            List<ShowSeat> showSeats = showSeatRepository.findByConfirmedBookingId(booking.getId());
            
            // Convert to response DTO
            BookingResponseDTO bookingResponse = convertToBookingResponseDTO(booking, showSeats);

            log.debug("Booking retrieved successfully: {}", bookingPublicId);

            return ResponseEntity.ok(BookingDetailResponse.builder()
                    .success(true)
                    .message("Booking retrieved successfully")
                    .booking(bookingResponse)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving booking: {}", bookingPublicId, e);
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Booking not found");
        }
    }

    @Override
    public ResponseEntity<BookingListResponse> getBookingsByShow(String showPublicId, Pageable pageable) {
        try {
            // Validate show public ID
            if (!StringUtils.hasText(showPublicId)) {
                return buildErrorListResponse(HttpStatus.BAD_REQUEST, ApiResponseMessage.INVALID_INPUT);
            }

            // Retrieve paginated bookings for the show
            Page<Booking> bookingsPage = bookingRepository.findByShowPublicId(showPublicId.trim(), pageable);
            
            // Convert to response DTOs with seat information
            List<BookingResponseDTO> bookingDTOs = bookingsPage.getContent().stream()
                    .map(booking -> {
                        List<ShowSeat> showSeats = showSeatRepository.findByConfirmedBookingId(booking.getId());
                        return convertToBookingResponseDTO(booking, showSeats);
                    })
                    .collect(Collectors.toList());

            // Build pagination info
            PaginationInfo pagination = buildPaginationInfo(bookingsPage);

            String message = bookingDTOs.isEmpty() ? 
                ApiResponseMessage.NO_DATA_FOUND : 
                "Bookings retrieved successfully";

            log.debug("Retrieved {} bookings for show: {}", bookingDTOs.size(), showPublicId);

            return ResponseEntity.ok(BookingListResponse.builder()
                    .success(true)
                    .message(message)
                    .bookings(bookingDTOs)
                    .pagination(pagination)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving bookings for show: {}", showPublicId, e);
            return buildErrorListResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve bookings");
        }
    }

    @Override
    public ResponseEntity<BookingListResponse> getBookingsByDate(LocalDate date, Pageable pageable) {
        try {
            // Validate date
            if (date == null) {
                return buildErrorListResponse(HttpStatus.BAD_REQUEST, "Date parameter is required");
            }

            // Calculate date range for the day
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            // Retrieve paginated bookings for the date
            Page<Booking> bookingsPage = bookingRepository.findByBookedAtBetween(startOfDay, endOfDay, pageable);
            
            // Convert to response DTOs
            List<BookingResponseDTO> bookingDTOs = bookingsPage.getContent().stream()
                    .map(booking -> {
                        List<ShowSeat> showSeats = showSeatRepository.findByConfirmedBookingId(booking.getId());
                        return convertToBookingResponseDTO(booking, showSeats);
                    })
                    .collect(Collectors.toList());

            // Build pagination info
            PaginationInfo pagination = buildPaginationInfo(bookingsPage);

            String message = bookingDTOs.isEmpty() ? 
                ApiResponseMessage.NO_DATA_FOUND : 
                "Bookings retrieved successfully";

            log.debug("Retrieved {} bookings for date: {}", bookingDTOs.size(), date);

            return ResponseEntity.ok(BookingListResponse.builder()
                    .success(true)
                    .message(message)
                    .bookings(bookingDTOs)
                    .pagination(pagination)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving bookings for date: {}", date, e);
            return buildErrorListResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve bookings");
        }
    }

    @Override
    public ResponseEntity<BookingListResponse> getAllBookingsWithFilters(String status, String paymentMode, 
                                                                        LocalDate fromDate, LocalDate toDate, 
                                                                        Pageable pageable) {
        try {
            Page<Booking> bookingsPage;
            
            // Apply filters based on provided parameters
            if (status != null && paymentMode != null && fromDate != null && toDate != null) {
                // All filters applied - status, payment mode, and date range
                LocalDateTime start = fromDate.atStartOfDay();
                LocalDateTime end = toDate.plusDays(1).atStartOfDay();
                bookingsPage = bookingRepository.findByStatusAndPaymentModeAndBookedAtBetween(
                    status, paymentMode, start, end, pageable);
            } else if (status != null && fromDate != null && toDate != null) {
                // Status and date range filters
                LocalDateTime start = fromDate.atStartOfDay();
                LocalDateTime end = toDate.plusDays(1).atStartOfDay();
                bookingsPage = bookingRepository.findByStatusAndBookedAtBetween(status, start, end, pageable);
            } else if (status != null) {
                // Only status filter
                bookingsPage = bookingRepository.findByStatus(status, pageable);
            } else if (fromDate != null && toDate != null) {
                // Only date range filter
                LocalDateTime start = fromDate.atStartOfDay();
                LocalDateTime end = toDate.plusDays(1).atStartOfDay();
                bookingsPage = bookingRepository.findByBookedAtBetween(start, end, pageable);
            } else {
                // No filters - retrieve all bookings
                bookingsPage = bookingRepository.findAll(pageable);
            }

            // Convert to response DTOs
            List<BookingResponseDTO> bookingDTOs = bookingsPage.getContent().stream()
                    .map(booking -> {
                        List<ShowSeat> showSeats = showSeatRepository.findByConfirmedBookingId(booking.getId());
                        return convertToBookingResponseDTO(booking, showSeats);
                    })
                    .collect(Collectors.toList());

            // Build pagination info
            PaginationInfo pagination = buildPaginationInfo(bookingsPage);

            String message = bookingDTOs.isEmpty() ? 
                ApiResponseMessage.NO_DATA_FOUND : 
                "Bookings retrieved successfully";

            log.debug("Retrieved {} bookings with filters", bookingDTOs.size());

            return ResponseEntity.ok(BookingListResponse.builder()
                    .success(true)
                    .message(message)
                    .bookings(bookingDTOs)
                    .pagination(pagination)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving filtered bookings", e);
            return buildErrorListResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve bookings");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<BookingDetailResponse> cancelBooking(String bookingPublicId, String reason) {
        try {
            // Validate booking public ID
            if (!StringUtils.hasText(bookingPublicId)) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiResponseMessage.INVALID_INPUT);
            }

            // Retrieve booking
            Booking booking = bookingRepository.findByPublicId(bookingPublicId.trim())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Validate booking can be cancelled
            if (!booking.isActive()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Booking is already cancelled or refunded");
            }

            // Check if show has already started (cannot cancel after show starts)
            if (booking.getShowStartTime().isBefore(LocalDateTime.now())) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Cannot cancel booking after show has started");
            }

            // Cancel booking and update database
            booking.cancel(reason);
            bookingRepository.save(booking);

            // Release associated seats for re-booking
            releaseSeatsForBooking(booking.getId());

            log.info("Booking cancelled successfully - Booking: {}, Reason: {}", bookingPublicId, reason);

            // Prepare response
            List<ShowSeat> showSeats = showSeatRepository.findByConfirmedBookingId(booking.getId());
            BookingResponseDTO bookingResponse = convertToBookingResponseDTO(booking, showSeats);

            return ResponseEntity.ok(BookingDetailResponse.builder()
                    .success(true)
                    .message("Booking cancelled successfully")
                    .booking(bookingResponse)
                    .build());

        } catch (Exception e) {
            log.error("Error cancelling booking: {}", bookingPublicId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to cancel booking");
        }
    }
    
    @Override
    @Transactional
    public ResponseEntity<BookingDetailResponse> cancelBookingGroup(String bookingGroupRef, String reason) {
        try {
            if (!StringUtils.hasText(bookingGroupRef)) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiResponseMessage.INVALID_INPUT);
            }

            // 1. Fetch all bookings in the group
            List<Booking> bookings = bookingRepository.findByBookingGroupRef(bookingGroupRef);

            if (bookings.isEmpty()) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Booking group not found");
            }

            // 2. Validate show time (same for all bookings)
            Booking firstBooking = bookings.get(0);
            if (firstBooking.getShowStartTime().isBefore(LocalDateTime.now())) {
                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Cannot cancel booking after show has started"
                );
            }

            // 3. Cancel all bookings in group
            for (Booking booking : bookings) {

                if (!booking.isActive()) {
                    continue; // already cancelled/refunded
                }

                booking.cancel(reason);
                bookingRepository.save(booking);

                // 4. Release seat
                releaseSeatsForBooking(booking.getId());
            }

            log.info("Booking group cancelled successfully - GroupRef: {}, Seats: {}",
                    bookingGroupRef, bookings.size());

            // 5. Prepare response (use first booking for response)
            List<ShowSeat> showSeats =
                    showSeatRepository.findByConfirmedBookingId(firstBooking.getId());

            BookingResponseDTO bookingResponse =
                    convertToBookingResponseDTO(firstBooking, showSeats);

            return ResponseEntity.ok(
                    BookingDetailResponse.builder()
                            .success(true)
                            .message("Booking cancelled successfully")
                            .booking(bookingResponse)
                            .build()
            );

        } catch (Exception e) {
            log.error("Error cancelling booking group: {}", bookingGroupRef, e);
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to cancel booking group"
            );
        }
    }


    @Override
    @Transactional
    public ResponseEntity<BookingDetailResponse> refundBooking(String bookingPublicId, String reason) {
        try {
            // Validate booking public ID and reason
            if (!StringUtils.hasText(bookingPublicId)) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiResponseMessage.INVALID_INPUT);
            }
            if (!StringUtils.hasText(reason)) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Refund reason is required");
            }

            // Retrieve booking
            Booking booking = bookingRepository.findByPublicId(bookingPublicId.trim())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Validate booking can be refunded
            if (!booking.isActive()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Booking is not active for refund");
            }

            // Process refund and update database
            booking.refund(reason);
            bookingRepository.save(booking);

            // Release associated seats
            releaseSeatsForBooking(booking.getId());

            log.info("Booking refund processed - Booking: {}, Reason: {}", bookingPublicId, reason);

            // Prepare response
            List<ShowSeat> showSeats = showSeatRepository.findByConfirmedBookingId(booking.getId());
            BookingResponseDTO bookingResponse = convertToBookingResponseDTO(booking, showSeats);

            return ResponseEntity.ok(BookingDetailResponse.builder()
                    .success(true)
                    .message("Booking refund processed successfully")
                    .booking(bookingResponse)
                    .build());

        } catch (Exception e) {
            log.error("Error processing refund for booking: {}", bookingPublicId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process refund");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<BookingDetailResponse> incrementPrintCount(String bookingPublicId) {
        try {
            // Validate booking public ID
            if (!StringUtils.hasText(bookingPublicId)) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiResponseMessage.INVALID_INPUT);
            }

            // Retrieve booking
            Booking booking = bookingRepository.findByPublicId(bookingPublicId.trim())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Increment print count
            booking.incrementPrintCount();
            bookingRepository.save(booking);

            log.debug("Print count incremented for booking: {}, new count: {}", 
                     bookingPublicId, booking.getPrintCount());

            // Prepare response
            List<ShowSeat> showSeats = showSeatRepository.findByConfirmedBookingId(booking.getId());
            BookingResponseDTO bookingResponse = convertToBookingResponseDTO(booking, showSeats);

            return ResponseEntity.ok(BookingDetailResponse.builder()
                    .success(true)
                    .message("Print count updated successfully")
                    .booking(bookingResponse)
                    .build());

        } catch (Exception e) {
            log.error("Error incrementing print count for booking: {}", bookingPublicId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update print count");
        }
    }

    @Override
    public ResponseEntity<BookingStatisticsResponse> getBookingStatistics(LocalDate fromDate, LocalDate toDate) {
        try {
            // Set default date range if not provided
            LocalDateTime startDate = (fromDate != null) ? fromDate.atStartOfDay() : LocalDate.now().atStartOfDay();
            LocalDateTime endDate = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : LocalDateTime.now();

            // Calculate statistics
            long totalBookings = bookingRepository.count();
            long todayBookings = bookingRepository.findTodaysBookings().size();
            
            BigDecimal totalRevenue = bookingRepository.calculateRevenueForPeriod(
                LocalDate.of(2020, 1, 1).atStartOfDay(), // From beginning of records
                LocalDateTime.now()
            );
            
            BigDecimal todayRevenue = bookingRepository.calculateRevenueForPeriod(
                LocalDate.now().atStartOfDay(),
                LocalDateTime.now()
            );

            long cancelledBookings = bookingRepository.findByStatus(BookingStatus.CANCELLED.getLabel()).size();
            long refundedBookings = bookingRepository.findByStatus(BookingStatus.REFUNDED.getLabel()).size();

            // Calculate payment method statistics
            PaymentMethodStats paymentStats = calculatePaymentMethodStats(startDate, endDate);

            log.debug("Booking statistics calculated - Total: {}, Today: {}, Revenue: {}", 
                     totalBookings, todayBookings, totalRevenue);

            return ResponseEntity.ok(BookingStatisticsResponse.builder()
                    .success(true)
                    .message("Booking statistics retrieved successfully")
                    .totalBookings(totalBookings)
                    .todayBookings(todayBookings)
                    .totalRevenue(totalRevenue)
                    .todayRevenue(todayRevenue)
                    .cancelledBookings(cancelledBookings)
                    .refundedBookings(refundedBookings)
                    .paymentMethodStats(paymentStats)
                    .build());

        } catch (Exception e) {
            log.error("Error calculating booking statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BookingStatisticsResponse.builder()
                            .success(false)
                            .message("Failed to retrieve booking statistics")
                            .build());
        }
    }

    // ========== PRIVATE HELPER METHODS ==========
    
    /**
     * Calculates VAT for a given base price.
     * Example: 1000 * 7.5% = 75
     */
    private BigDecimal calculateVat(BigDecimal basePrice) {
        return basePrice
                .multiply(vatPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }


    /**
     * Validates booking request parameters.
     * Returns error response if validation fails, null if validation passes.
     */
    private ResponseEntity<BookingDetailResponse> validateBookingRequest(BookingRequestDTO bookingRequest) {
        if (bookingRequest == null) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiResponseMessage.INVALID_INPUT);
        }
        if (!StringUtils.hasText(bookingRequest.getShowPublicId())) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Show public ID is required");
        }
        if (bookingRequest.getSeatPublicIds() == null || bookingRequest.getSeatPublicIds().isEmpty()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "At least one seat must be selected");
        }
        if (!StringUtils.hasText(bookingRequest.getPaymentMode())) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Payment mode is required");
        }
        
        // Validate payment mode
        try {
            PaymentMode.valueOf(bookingRequest.getPaymentMode().toUpperCase());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid payment mode");
        }

        return null; // Validation passed
    }

    /**
     * Validates and retrieves show seats for booking.
     * Throws exception if any seat is invalid or not found.
     */
    private List<ShowSeat> validateAndGetShowSeats(List<String> seatPublicIds, Long showId) {
        List<ShowSeat> showSeats = showSeatRepository.findByShowIdAndPublicIds(showId, seatPublicIds);
        
        if (showSeats.size() != seatPublicIds.size()) {
            throw new RuntimeException("One or more seats not found for this show");
        }
        
        return showSeats;
    }

    /**
     * Checks if all seats are valid for booking for the CURRENT logged-in user.
     *
     * BUSINESS RULES ENFORCED:
     * - Seat must be in HELD state
     * - Seat hold must NOT be expired (expiresAt > now)
     * - Seat must be held by SAME user who is confirming booking
     *
     * @param showSeats list of seats to be booked
     * @param bookingUserIdentifier identifier of current user (e.g. email / username from SecurityContext)
     */
    private boolean areSeatsAvailableForBooking(List<ShowSeat> showSeats, String bookingUserIdentifier) {
        LocalDateTime now = LocalDateTime.now();

        return showSeats.stream().allMatch(seat -> {
            // Must be in HELD state
            if (!ShowSeatState.HELD.getLabel().equals(seat.getState())) {
                log.warn("Seat not in HELD state at booking time - Seat: {}, State: {}",
                        seat.getSeatPublicId(), seat.getState());
                return false;
            }

            // Must not be expired
            if (seat.getExpiresAt() == null || !seat.getExpiresAt().isAfter(now)) {
                log.warn("Seat hold expired before booking - Seat: {}, ExpiresAt: {}",
                        seat.getSeatPublicId(), seat.getExpiresAt());
                return false;
            }

            // Must be held by the same user
            if (bookingUserIdentifier == null ||
                    seat.getReservedBy() == null ||
                    !seat.getReservedBy().equals(bookingUserIdentifier)) {

                log.warn("Seat held by different user - Seat: {}, HeldBy: {}, BookingBy: {}",
                        seat.getSeatPublicId(), seat.getReservedBy(), bookingUserIdentifier);
                return false;
            }

            return true;
        });
    }

    /**
     * Checks if show is active and in the future.
     */
    private boolean isShowActiveAndFuture(Show show) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime cutoff = show.getStartAt().minusMinutes(MIN_MINUTES_BEFORE_SHOW_FOR_BOOKING);
        
        // ✅ Allow both SCHEDULED and RUNNING shows
        boolean isBookableStatus = 
            show.getStatus() == ShowStatus.SCHEDULED.getCode() || 
            show.getStatus() == ShowStatus.RUNNING.getCode();
        
        boolean isFuture = cutoff.isAfter(now);
        
        System.out.println("DEBUG - Status: " + show.getStatus() + " | Bookable: " + isBookableStatus + " | Future: " + isFuture);
        
        return isBookableStatus && isFuture;
    }

    /**
     * Creates booking records in database.
     * Creates one booking per seat for individual tracking.
     */
    private List<Booking> createBookingRecords(
            BookingRequestDTO bookingRequest,
            Show show,
            List<ShowSeat> showSeats,
            Long bookedByUserId) {
        List<Booking> bookings = new ArrayList<>();

        // 🔑 Same reference for all seats booked together
        String bookingGroupRef = UUID.randomUUID().toString();

        for (ShowSeat showSeat : showSeats) {

            // 1️⃣ Base seat price (NO tax)
            BigDecimal basePrice = showSeat.getPrice();

            // 2️⃣ VAT calculation
            BigDecimal vatAmount = calculateVat(basePrice);

            // 3️⃣ Final amount customer paid
            BigDecimal totalAmount = basePrice.add(vatAmount);

            Booking booking = Booking.builder()
                    .publicId(UUID.randomUUID().toString())

                    // 🔗 Group reference (same for all seats in this booking)
                    .bookingGroupRef(bookingGroupRef)

                    // Show & seat references
                    .showPublicId(bookingRequest.getShowPublicId())
                    .seatPublicId(showSeat.getSeatPublicId())

                    // Snapshot show data
                    .showStartTime(show.getStartAt())
                    .showEndTime(show.getEndAt())
                    .screenName(show.getScreenPublicId())

                    // 💰 Pricing snapshot
                    .price(basePrice)          // base price
                    .vatAmount(vatAmount)      // VAT
                    .totalAmount(totalAmount)  // final paid amount

                    // Payment & audit
                    .paymentMode(bookingRequest.getPaymentMode().toUpperCase())
                    .transactionReference(bookingRequest.getTransactionReference())
                    .bookedByUserId(bookedByUserId)
                    .bookedAt(LocalDateTime.now())
                    .status(BookingStatus.ISSUED.getLabel())
                    .printCount(0)
                    .notes(bookingRequest.getNotes())
                    .build();

            bookings.add(bookingRepository.save(booking));
        }

        return bookings;
    }


    /**
     * Updates show seat states to SOLD, clears hold info and confirms seat holds.
     * Also broadcasts seat state change so all POS seat maps update to "BOOKED".
     */
    private void updateShowSeatStates(List<ShowSeat> showSeats, Booking booking) {

        String showPublicId = booking.getShowPublicId();
        LocalDateTime now = LocalDateTime.now();

        for (ShowSeat showSeat : showSeats) {

            // 1. Mark as SOLD and link to booking
            showSeat.setState(ShowSeatState.SOLD.getLabel());
            showSeat.setConfirmedBookingId(booking.getId());

            // 2. Clear temporary hold data so scheduler will never touch it again
            showSeat.setReservedBy(null);
            showSeat.setReservedAt(null);
            showSeat.setExpiresAt(null);

            showSeatRepository.save(showSeat);

            // 3. Remove from in-memory hold tracking (SeatHoldService)
            seatHoldService.confirmSeatHold(showSeat.getSeatPublicId());

            // 4. Broadcast seat SOLD event so all POS clients update UI
            SeatStateEvent seatSoldEvent = SeatStateEvent.builder()
                    .showPublicId(showPublicId)
                    .seatPublicId(showSeat.getPublicId())
                    .state(ShowSeatState.SOLD.getLabel())
                    .eventType("SEAT_SOLD")
                    .timestamp(now)
                    .build();

            webSocketService.broadcastSeatUpdate(showPublicId, seatSoldEvent);
        }
    }


    /**
     * Releases seats associated with a booking (on cancellation/refund).
     * (NOTE: You can extend this to broadcast seat release via WebSocket if needed.)
     */
    private void releaseSeatsForBooking(Long bookingId) {
        List<ShowSeat> showSeats = showSeatRepository.findByConfirmedBookingId(bookingId);

        for (ShowSeat showSeat : showSeats) {

            showSeat.setState(ShowSeatState.AVAILABLE.getLabel());
            showSeat.setConfirmedBookingId(null);
            showSeat.setReservedBy(null);
            showSeat.setReservedAt(null);
            showSeat.setExpiresAt(null);

            showSeatRepository.save(showSeat);

            // 🔥 BROADCAST seat release to ALL POS systems
            String showPublicId = showRepository.findById(showSeat.getShowId())
                    .map(Show::getPublicId)
                    .orElse(null);

            if (showPublicId != null) {
                SeatStateEvent event = SeatStateEvent.builder()
                        .showPublicId(showPublicId)
                        .seatPublicId(showSeat.getPublicId())
                        .state(ShowSeatState.AVAILABLE.getLabel())
                        .eventType("SEAT_RELEASED")
                        .timestamp(LocalDateTime.now())
                        .build();

                webSocketService.broadcastSeatUpdate(showPublicId, event);
            }
        }
    }


    /**
     * Broadcasts booking confirmation to all POS systems.
     */
    private void broadcastBookingConfirmation(List<Booking> bookings, List<ShowSeat> showSeats) {
        try {
            Booking booking = bookings.get(0); // representative booking

            List<String> seatLabels = showSeats.stream()
                    .map(ShowSeat::getSeatLabel)
                    .collect(Collectors.toList());

            // ✅ CORRECT: sum of PAID AMOUNT (base + VAT)
            BigDecimal totalAmount = bookings.stream()
                    .map(Booking::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BookingConfirmationEvent confirmationEvent = BookingConfirmationEvent.builder()
                    .bookingPublicId(booking.getPublicId())
                    .showPublicId(booking.getShowPublicId())
                    .seatLabels(seatLabels)
                    .totalAmount(totalAmount)
                    .paymentMode(booking.getPaymentMode())
                    .timestamp(LocalDateTime.now())
                    .bookedBy("Counter Staff")
                    .build();

            webSocketService.broadcastBookingConfirmation(confirmationEvent);

        } catch (Exception e) {
            log.error("Error broadcasting booking confirmation", e);
        }
    }


    /**
     * Calculates payment method statistics for the given period.
     */
    private PaymentMethodStats calculatePaymentMethodStats(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> revenueByPaymentMode = bookingRepository.calculateRevenueByPaymentMode(startDate, endDate);
        
        PaymentMethodStats.PaymentMethodStatsBuilder statsBuilder = PaymentMethodStats.builder();
        
        for (Object[] row : revenueByPaymentMode) {
            String paymentMode = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            
            switch (paymentMode.toUpperCase()) {
                case "CASH":
                    statsBuilder.cashCount(1).cashAmount(amount);
                    break;
                case "POS":
                    statsBuilder.posCount(1).posAmount(amount);
                    break;
                case "TRANSFER":
                    statsBuilder.transferCount(1).transferAmount(amount);
                    break;
                case "ONLINE":
                    statsBuilder.onlineCount(1).onlineAmount(amount);
                    break;
            }
        }
        
        return statsBuilder.build();
    }

    /**
     * Retrieves current authenticated user from security context.
     */
    private User getCurrentUser(String userEmail) {
        return userRepository.findByEmailId(userEmail).orElse(null);
    }

    /**
     * Converts Booking entity to response DTO.
     */
    private BookingResponseDTO convertToBookingResponseDTO(Booking booking, List<ShowSeat> showSeats) {

        List<String> seatLabels = showSeats.stream()
                .map(ShowSeat::getSeatLabel)
                .collect(Collectors.toList());

        return BookingResponseDTO.builder()
                .bookingPublicId(booking.getPublicId())
                .bookingGroupRef(booking.getBookingGroupRef())

                .showPublicId(booking.getShowPublicId())
                .movieTitle("Movie Title") // TODO: lookup later
                .screenName(booking.getScreenName())

                // 💰 PRICING BREAKDOWN (FIXED)
                .baseAmount(booking.getPrice())
                .vatAmount(booking.getVatAmount())
                .totalAmount(booking.getTotalAmount())

                .paymentMode(booking.getPaymentMode())
                .transactionReference(booking.getTransactionReference())
                .status(booking.getStatus())
                .bookedAt(booking.getBookedAt())
                .showStartTime(booking.getShowStartTime())
                .showEndTime(booking.getShowEndTime())

                .seatLabels(seatLabels)
                .printCount(booking.getPrintCount())
                .qrCodeData(generateQRCodeData(booking))
                .build();
    }


    /**
     * Generates QR code data for ticket printing.
     */
    private String generateQRCodeData(Booking booking) {
        // Simple QR code data - can be enhanced with encryption
        return String.format("BOOKING:%s:SHOW:%s:SEAT:%s", 
                booking.getPublicId(), booking.getShowPublicId(), booking.getSeatPublicId());
    }

    /**
     * Builds pagination info from Spring Data Page.
     */
    private PaginationInfo buildPaginationInfo(Page<?> page) {
        return PaginationInfo.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Builds error response for single booking operations.
     */
    private ResponseEntity<BookingDetailResponse> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(BookingDetailResponse.builder()
                        .success(false)
                        .message(message)
                        .booking(null)
                        .build());
    }

    /**
     * Builds error response for list booking operations.
     */
    private ResponseEntity<BookingListResponse> buildErrorListResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(BookingListResponse.builder()
                        .success(false)
                        .message(message)
                        .bookings(Collections.emptyList())
                        .pagination(null)
                        .build());
    }
}
