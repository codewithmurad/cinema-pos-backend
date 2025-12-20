package com.telecamnig.cinemapos.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.telecamnig.cinemapos.dto.BookingDetailResponse;
import com.telecamnig.cinemapos.dto.BookingListResponse;
import com.telecamnig.cinemapos.dto.BookingRequestDTO;
import com.telecamnig.cinemapos.dto.BookingStatisticsResponse;

import java.time.LocalDate;

/**
 * Service interface for managing cinema bookings.
 * 
 * Handles:
 * - Booking creation with seat validation
 * - Payment processing (Cash/Online)
 * - Booking cancellation and refunds
 * - Real-time synchronization across POS systems
 * - Ticket generation and printing
 * 
 * @author Your Name
 * @version 1.0
 */
public interface BookingService {
    
    /**
     * Create a new booking for selected seats.
     * Validates seat availability, processes payment, and updates real-time state.
     * 
     * @param bookingRequest The booking request with customer and seat details
     * @return ResponseEntity with booking confirmation or error
     */
    ResponseEntity<BookingDetailResponse> createBooking(BookingRequestDTO bookingRequest);
    
    /**
     * Retrieve booking details by public ID.
     * Used for ticket lookup and booking management.
     * 
     * @param bookingPublicId The unique public ID of the booking
     * @return ResponseEntity with booking details or error
     */
    ResponseEntity<BookingDetailResponse> getBookingByPublicId(String bookingPublicId);
    
    /**
     * Get all bookings for a specific show with pagination.
     * Used by counter staff to view show bookings.
     * 
     * @param showPublicId The show public ID
     * @param pageable Pagination information
     * @return ResponseEntity with paginated booking list
     */
    ResponseEntity<BookingListResponse> getBookingsByShow(String showPublicId, Pageable pageable);
    
    /**
     * Get bookings for a specific date with pagination.
     * Used for daily reports and management.
     * 
     * @param date The date to filter bookings
     * @param pageable Pagination information
     * @return ResponseEntity with paginated booking list
     */
    ResponseEntity<BookingListResponse> getBookingsByDate(LocalDate date, Pageable pageable);
    
    /**
     * Get all bookings with optional filters for admin reporting.
     * Supports filtering by status, payment mode, and date range.
     * 
     * @param status Optional booking status filter
     * @param paymentMode Optional payment mode filter
     * @param fromDate Optional start date filter
     * @param toDate Optional end date filter
     * @param pageable Pagination information
     * @return ResponseEntity with filtered paginated booking list
     */
    ResponseEntity<BookingListResponse> getAllBookingsWithFilters(String status, String paymentMode, 
                                                                LocalDate fromDate, LocalDate toDate, 
                                                                Pageable pageable);
    
    /**
     * Cancel an active booking.
     * Releases seats and updates booking status to CANCELLED.
     * 
     * @param bookingPublicId The booking to cancel
     * @param reason Optional reason for cancellation
     * @return ResponseEntity with updated booking details
     */
    ResponseEntity<BookingDetailResponse> cancelBooking(String bookingPublicId, String reason);
    
    /**
     * Cancel an active booking GROUP.
     * Cancels all seats booked together using bookingGroupRef.
     *
     * @param bookingGroupRef group reference of booking
     * @param reason optional cancel reason
     * @return response with cancelled booking details
     */
    ResponseEntity<BookingDetailResponse> cancelBookingGroup(String bookingGroupRef, String reason);
    
    /**
     * Process refund for a booking.
     * Updates booking status to REFUNDED and releases seats.
     * Admin only operation.
     * 
     * @param bookingPublicId The booking to refund
     * @param reason Reason for refund
     * @return ResponseEntity with updated booking details
     */
    ResponseEntity<BookingDetailResponse> refundBooking(String bookingPublicId, String reason);
    
    /**
     * Increment print count when ticket is printed.
     * Tracks how many times a ticket has been printed.
     * 
     * @param bookingPublicId The booking to update
     * @return ResponseEntity with updated booking details
     */
    ResponseEntity<BookingDetailResponse> incrementPrintCount(String bookingPublicId);
    
    /**
     * Get booking statistics for dashboard and reporting.
     * Provides revenue, booking counts, and payment method analysis.
     * 
     * @param fromDate Optional start date for statistics
     * @param toDate Optional end date for statistics
     * @return ResponseEntity with booking statistics
     */
    ResponseEntity<BookingStatisticsResponse> getBookingStatistics(LocalDate fromDate, LocalDate toDate);

}