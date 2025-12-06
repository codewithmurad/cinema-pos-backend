package com.telecamnig.cinemapos.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

            // 7. Calculate total booking amount
            BigDecimal totalAmount = calculateTotalAmount(showSeats);

            // 8. Create booking records and update database
            List<Booking> bookings = createBookingRecords(
                    bookingRequest, show, showSeats, totalAmount, currentUser.getId()
            );

            // 9. Update show seat states to SOLD and confirm holds
            updateShowSeatStates(showSeats, bookings.get(0));

            // 10. Broadcast booking confirmation to all POS systems
            broadcastBookingConfirmation(bookings.get(0), showSeats);

            // 11. Prepare and return success response
            BookingResponseDTO bookingResponse = convertToBookingResponseDTO(bookings.get(0), showSeats);

            log.info("Booking created successfully - Booking: {}, Show: {}, Amount: {}, Seats: {}", 
                    bookings.get(0).getPublicId(), show.getPublicId(), totalAmount, 
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
        if (!StringUtils.hasText(bookingRequest.getCustomerName())) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Customer name is required");
        }
        if (!StringUtils.hasText(bookingRequest.getCustomerPhone())) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Customer phone is required");
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
     * Calculates total booking amount from selected seats.
     */
    private BigDecimal calculateTotalAmount(List<ShowSeat> showSeats) {
        return showSeats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Creates booking records in database.
     * Creates one booking per seat for individual tracking.
     */
    private List<Booking> createBookingRecords(BookingRequestDTO bookingRequest, Show show, 
                                             List<ShowSeat> showSeats, BigDecimal totalAmount, Long bookedByUserId) {
        List<Booking> bookings = new ArrayList<>();
        
        for (ShowSeat showSeat : showSeats) {
            Booking booking = Booking.builder()
                    .publicId(UUID.randomUUID().toString())
                    .showPublicId(bookingRequest.getShowPublicId())
                    .seatPublicId(showSeat.getSeatPublicId())
                    .showStartTime(show.getStartAt())
                    .showEndTime(show.getEndAt())
                    .screenName(show.getScreenPublicId()) // Would need screen name lookup
                    .price(showSeat.getPrice())
                    .paymentMode(bookingRequest.getPaymentMode().toUpperCase())
                    .transactionReference(bookingRequest.getTransactionReference())
                    .customerName(bookingRequest.getCustomerName())
                    .customerPhone(bookingRequest.getCustomerPhone())
                    .customerEmail(bookingRequest.getCustomerEmail())
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
            showSeatRepository.save(showSeat);
            
            // TODO (optional for prod):
            // Broadcast seat release to all POS systems so UIs update in real-time
            // You'd need showPublicId (can resolve from showRepository using showId).
        }
    }

    /**
     * Broadcasts booking confirmation to all POS systems.
     */
    private void broadcastBookingConfirmation(Booking booking, List<ShowSeat> showSeats) {
        try {
            List<String> seatLabels = showSeats.stream()
                    .map(ShowSeat::getSeatLabel)
                    .collect(Collectors.toList());
            
            // Calculate total amount for all seats (aggregation for multiple seats)
            BigDecimal totalAmount = showSeats.stream()
                    .map(ShowSeat::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BookingConfirmationEvent confirmationEvent = BookingConfirmationEvent.builder()
                    .bookingPublicId(booking.getPublicId())
                    .showPublicId(booking.getShowPublicId())
                    .customerName(booking.getCustomerName())
                    .seatLabels(seatLabels)
                    .totalAmount(totalAmount)  // Use aggregated amount
                    .paymentMode(booking.getPaymentMode())
                    .timestamp(LocalDateTime.now())
                    .bookedBy("Counter Staff")  // You can get this from user context
                    .build();
            
            webSocketService.broadcastBookingConfirmation(confirmationEvent);
            
        } catch (Exception e) {
            log.error("Error broadcasting booking confirmation: {}", booking.getPublicId(), e);
            // Don't throw exception - booking should still be created
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
                .showPublicId(booking.getShowPublicId())
                .movieTitle("Movie Title") // Would need movie lookup
                .screenName(booking.getScreenName())
                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())
                .customerEmail(booking.getCustomerEmail())
                .totalAmount(booking.getPrice())
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
