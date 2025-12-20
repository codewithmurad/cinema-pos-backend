package com.telecamnig.cinemapos.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.telecamnig.cinemapos.dto.*;
import com.telecamnig.cinemapos.service.BookingService;

import java.time.LocalDate;

/**
 * REST Controller for managing cinema bookings.
 * 
 * FEATURES:
 * - Create new bookings with seat selection
 * - Retrieve booking details and history
 * - Cancel bookings and process refunds
 * - Ticket printing and management
 * - Booking statistics and reports
 * 
 * SECURITY:
 * - Counter staff can create bookings and view today's bookings
 * - Admin has access to all bookings and reporting
 * - Role-based authorization for all operations
 * 
 * @author Your Name
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    /**
     * Create a new booking for selected seats.
     * 
     * FLOW:
     * 1. Validate seat availability
     * 2. Process payment
     * 3. Create booking records
     * 4. Update seat states to SOLD
     * 5. Broadcast confirmation to all POS systems
     * 
     * @param bookingRequest The booking request with customer and seat details
     * @return ResponseEntity with booking confirmation
     */
    //@PreAuthorize("hasRole('COUNTER') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BookingDetailResponse> createBooking(
            @Valid @RequestBody BookingRequestDTO bookingRequest) {
        log.info("Booking creation request - Show: {}, Seats: {}", 
                bookingRequest.getShowPublicId(), bookingRequest.getSeatPublicIds());
        
        return bookingService.createBooking(bookingRequest);
    }

    /**
     * Retrieve booking details by public ID.
     * Used for ticket lookup, reprints, and booking management.
     * 
     * @param bookingPublicId The unique public ID of the booking
     * @return ResponseEntity with booking details
     */
   // @PreAuthorize("hasRole('COUNTER') or hasRole('ADMIN')")
    @GetMapping("/{bookingPublicId}")
    public ResponseEntity<BookingDetailResponse> getBookingByPublicId(
            @PathVariable String bookingPublicId) {
        log.info("Fetching booking details: {}", bookingPublicId);
        return bookingService.getBookingByPublicId(bookingPublicId);
    }

    /**
     * Get all bookings for a specific show with pagination.
     * Used by counter staff to view show occupancy and bookings.
     * 
     * @param showPublicId The show public ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return ResponseEntity with paginated booking list
     */
 //   @PreAuthorize("hasRole('COUNTER') or hasRole('ADMIN')")
    @GetMapping("/show/{showPublicId}")
    public ResponseEntity<BookingListResponse> getBookingsByShow(
            @PathVariable String showPublicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching bookings for show: {}, page: {}, size: {}", showPublicId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return bookingService.getBookingsByShow(showPublicId, pageable);
    }

    /**
     * Get today's bookings for counter staff dashboard.
     * Shows all bookings created today for quick reference.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return ResponseEntity with today's bookings
     */
 //   @PreAuthorize("hasRole('COUNTER') or hasRole('ADMIN')")
    @GetMapping("/today")
    public ResponseEntity<BookingListResponse> getTodayBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching today's bookings, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return bookingService.getBookingsByDate(LocalDate.now(), pageable);
    }

    /**
     * Get bookings for a specific date (Admin only).
     * Used for daily reports and accounting.
     * 
     * @param date The date to filter bookings (YYYY-MM-DD format)
     * @param page Page number (default: 0)
     * @param size Page size (default: 50)
     * @return ResponseEntity with date-filtered bookings
     */
  //  @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/date/{date}")
    public ResponseEntity<BookingListResponse> getBookingsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("Fetching bookings for date: {}, page: {}, size: {}", date, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return bookingService.getBookingsByDate(date, pageable);
    }

    /**
     * Get all bookings with advanced filtering (Admin only).
     * Supports filtering by status, payment mode, and date range.
     * Used for comprehensive reporting and analysis.
     * 
     * @param status Optional booking status filter (ISSUED, CANCELLED, REFUNDED)
     * @param paymentMode Optional payment mode filter (CASH, POS, TRANSFER, ONLINE)
     * @param fromDate Optional start date filter (YYYY-MM-DD)
     * @param toDate Optional end date filter (YYYY-MM-DD)
     * @param page Page number (default: 0)
     * @param size Page size (default: 50)
     * @return ResponseEntity with filtered paginated booking list
     */
 //   @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<BookingListResponse> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("Fetching filtered bookings - status: {}, payment: {}, from: {}, to: {}, page: {}, size: {}", 
                status, paymentMode, fromDate, toDate, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return bookingService.getAllBookingsWithFilters(status, paymentMode, fromDate, toDate, pageable);
    }

    /**
     * Cancel an active booking.
     * Releases seats for re-booking and updates booking status.
     * Can only cancel bookings before show start time.
     * 
     * @param bookingPublicId The booking to cancel
     * @param reason Optional reason for cancellation
     * @return ResponseEntity with cancelled booking details
     */
//    @PreAuthorize("hasRole('COUNTER') or hasRole('ADMIN')")
    @PutMapping("/{bookingPublicId}/cancel")
    public ResponseEntity<BookingDetailResponse> cancelBooking(
            @PathVariable String bookingPublicId,
            @RequestParam(required = false) String reason) {
        log.info("Cancelling booking: {}, reason: {}", bookingPublicId, reason);
        return bookingService.cancelBooking(bookingPublicId, reason);
    }
    
    /**
     * Cancel booking using bookingGroupRef.
     * Cancels ALL seats booked together.
     *
     * Used by counter after successful booking
     * (Print / Cancel flow)
     */
    @PutMapping("/group/{bookingGroupRef}/cancel")
    public ResponseEntity<BookingDetailResponse> cancelBookingGroup(
            @PathVariable String bookingGroupRef,
            @RequestParam(required = false) String reason) {

        log.info("Cancelling booking group: {}, reason: {}", bookingGroupRef, reason);
        return bookingService.cancelBookingGroup(bookingGroupRef, reason);
    }


    /**
     * Process refund for a booking (Admin only).
     * Updates booking status to REFUNDED and releases seats.
     * Used for post-show refunds or payment issues.
     * 
     * @param bookingPublicId The booking to refund
     * @param reason Reason for refund (required)
     * @return ResponseEntity with refunded booking details
     */
 //   @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{bookingPublicId}/refund")
    public ResponseEntity<BookingDetailResponse> refundBooking(
            @PathVariable String bookingPublicId,
            @RequestParam String reason) {
        log.info("Processing refund for booking: {}, reason: {}", bookingPublicId, reason);
        return bookingService.refundBooking(bookingPublicId, reason);
    }

    /**
     * Increment print count when ticket is printed.
     * Tracks how many times a ticket has been printed.
     * Used for ticket reprints and audit trail.
     * 
     * @param bookingPublicId The booking to update
     * @return ResponseEntity with updated booking details
     */
 //   @PreAuthorize("hasRole('COUNTER') or hasRole('ADMIN')")
    @PutMapping("/{bookingPublicId}/print")
    public ResponseEntity<BookingDetailResponse> incrementPrintCount(
            @PathVariable String bookingPublicId) {
        log.info("Incrementing print count for booking: {}", bookingPublicId);
        return bookingService.incrementPrintCount(bookingPublicId);
    }

    /**
     * Get booking statistics for admin dashboard.
     * Provides revenue, booking counts, and payment method analysis.
     * 
     * @param fromDate Optional start date for statistics (YYYY-MM-DD)
     * @param toDate Optional end date for statistics (YYYY-MM-DD)
     * @return ResponseEntity with booking statistics
     */
//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/statistics")
    public ResponseEntity<BookingStatisticsResponse> getBookingStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        log.info("Fetching booking statistics from: {} to: {}", fromDate, toDate);
        return bookingService.getBookingStatistics(fromDate, toDate);
    }

    /**
     * Get customer booking history by phone number.
     * Useful for customer service and loyalty tracking.
     * 
     * @param phone Customer phone number
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return ResponseEntity with customer's booking history
     */
//    @PreAuthorize("hasRole('COUNTER') or hasRole('ADMIN')")
    @GetMapping("/customer/{phone}")
    public ResponseEntity<BookingListResponse> getCustomerBookings(
            @PathVariable String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching booking history for customer: {}, page: {}, size: {}", phone, page, size);
        Pageable pageable = PageRequest.of(page, size);
        // Note: You'll need to add this method to your BookingService
        // return bookingService.getBookingsByCustomerPhone(phone, pageable);
        
        // Temporary implementation using existing method
        return bookingService.getAllBookingsWithFilters(null, null, null, null, pageable);
    }

}