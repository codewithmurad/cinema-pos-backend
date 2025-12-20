package com.telecamnig.cinemapos.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telecamnig.cinemapos.entity.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ========== BASIC FINDERS ==========
    
    /**
     * Find booking by publicId (for ticket lookup and API calls).
     * Indexed: idx_bookings_publicid
     */
    Optional<Booking> findByPublicId(String publicId);

    /**
     * Find all bookings for a specific show.
     * Indexed: idx_bookings_showpublicid
     */
    List<Booking> findByShowPublicId(String showPublicId);

    /**
     * Find all bookings for a specific seat across all shows.
     * Indexed: idx_bookings_seatpublicid
     */
    List<Booking> findBySeatPublicId(String seatPublicId);

    /**
     * Find booking by show and seat (for duplicate prevention).
     * Uses unique constraint: uq_booking_show_seat
     */
    Optional<Booking> findByShowPublicIdAndSeatPublicId(String showPublicId, String seatPublicId);

    // ========== STATUS-BASED FILTERS ==========
    
    /**
     * Find bookings by status.
     * Indexed: idx_bookings_status_bookedat
     */
    List<Booking> findByStatus(String status);

    /**
     * Find bookings by show and status.
     */
    List<Booking> findByShowPublicIdAndStatus(String showPublicId, String status);

    /**
     * Find active bookings (ISSUED status) for a show.
     */
    @Query("SELECT b FROM Booking b WHERE b.showPublicId = :showPublicId AND b.status = 'ISSUED'")
    List<Booking> findActiveBookingsByShow(@Param("showPublicId") String showPublicId);

    /**
     * Find cancelled/refunded bookings for a show.
     */
    @Query("SELECT b FROM Booking b WHERE b.showPublicId = :showPublicId AND b.status IN ('CANCELLED', 'REFUNDED')")
    List<Booking> findCancelledBookingsByShow(@Param("showPublicId") String showPublicId);

    // ========== TIME-BASED FILTERS ==========
    
    /**
     * Find bookings within a date range.
     * Indexed: idx_bookings_bookedat
     */
    List<Booking> findByBookedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find bookings by date range and status.
     */
    List<Booking> findByBookedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, String status);

    /**
     * Find today's bookings.
     */
    @Query("SELECT b FROM Booking b WHERE DATE(b.bookedAt) = CURRENT_DATE")
    List<Booking> findTodaysBookings();

    /**
     * Find upcoming bookings (show not started yet).
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'ISSUED' AND b.showStartTime > CURRENT_TIMESTAMP")
    List<Booking> findUpcomingBookings();

    /**
     * Find expired bookings (show already completed).
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'ISSUED' AND b.showEndTime < CURRENT_TIMESTAMP")
    List<Booking> findExpiredBookings();

    // ========== PAYMENT & REVENUE QUERIES ==========
    
    /**
     * Find bookings by payment mode.
     * Indexed: idx_bookings_payment_bookedat
     */
    List<Booking> findByPaymentMode(String paymentMode);

    /**
     * Find bookings by payment mode and date range.
     */
    List<Booking> findByPaymentModeAndBookedAtBetween(String paymentMode, LocalDateTime start, LocalDateTime end);

    /**
     * Find bookings by staff member.
     */
    List<Booking> findByBookedByUserId(Long bookedByUserId);

    /**
     * Find bookings by staff and date range.
     */
    List<Booking> findByBookedByUserIdAndBookedAtBetween(Long bookedByUserId, LocalDateTime start, LocalDateTime end);

    // ========== PAGINATION METHODS ==========
    
    /**
     * Find bookings with pagination.
     */
    Page<Booking> findAll(Pageable pageable);

    /**
     * Find bookings by status with pagination.
     */
    Page<Booking> findByStatus(String status, Pageable pageable);

    /**
     * Find bookings by date range with pagination.
     */
    Page<Booking> findByBookedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Find bookings by show with pagination.
     */
    Page<Booking> findByShowPublicId(String showPublicId, Pageable pageable);

    /**
     * Find bookings by status and date range with pagination.
     */
    Page<Booking> findByStatusAndBookedAtBetween(String status, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Find bookings by status and payment mode with pagination.
     */
    Page<Booking> findByStatusAndPaymentMode(String status, String paymentMode, Pageable pageable);

    /**
     * Find bookings by status, payment mode and date range with pagination.
     */
    Page<Booking> findByStatusAndPaymentModeAndBookedAtBetween(String status, String paymentMode, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // ========== REVENUE & REPORTING QUERIES ==========
    
    /**
     * Calculate total revenue for a period.
     */
    @Query("SELECT COALESCE(SUM(b.price), 0) FROM Booking b WHERE b.status = 'ISSUED' AND b.bookedAt BETWEEN :start AND :end")
    BigDecimal calculateRevenueForPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Calculate revenue by payment mode for a period.
     */
    @Query("SELECT b.paymentMode, COALESCE(SUM(b.price), 0) FROM Booking b WHERE b.status = 'ISSUED' AND b.bookedAt BETWEEN :start AND :end GROUP BY b.paymentMode")
    List<Object[]> calculateRevenueByPaymentMode(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Calculate daily revenue summary.
     */
    @Query("SELECT DATE(b.bookedAt), b.paymentMode, COUNT(b), COALESCE(SUM(b.price), 0) " +
           "FROM Booking b WHERE b.status = 'ISSUED' AND b.bookedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(b.bookedAt), b.paymentMode ORDER BY DATE(b.bookedAt) DESC")
    List<Object[]> getDailyRevenueSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Calculate screen-wise revenue.
     */
    @Query("SELECT b.screenName, COUNT(b), COALESCE(SUM(b.price), 0) " +
           "FROM Booking b WHERE b.status = 'ISSUED' AND b.bookedAt BETWEEN :start AND :end " +
           "GROUP BY b.screenName ORDER BY SUM(b.price) DESC")
    List<Object[]> getRevenueByScreen(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Calculate staff performance.
     */
    @Query("SELECT b.bookedByUserId, COUNT(b), COALESCE(SUM(b.price), 0) " +
           "FROM Booking b WHERE b.status = 'ISSUED' AND b.bookedAt BETWEEN :start AND :end " +
           "GROUP BY b.bookedByUserId ORDER BY SUM(b.price) DESC")
    List<Object[]> getStaffPerformance(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ========== VALIDATION & EXISTS CHECKS ==========
    
    /**
     * Check if publicId exists.
     */
    boolean existsByPublicId(String publicId);

    /**
     * Check if booking exists for show and seat.
     */
    boolean existsByShowPublicIdAndSeatPublicId(String showPublicId, String seatPublicId);

    /**
     * Check if active booking exists for show and seat.
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.showPublicId = :showPublicId AND b.seatPublicId = :seatPublicId AND b.status = 'ISSUED'")
    boolean existsActiveBookingForSeat(@Param("showPublicId") String showPublicId, @Param("seatPublicId") String seatPublicId);

    // ========== BULK OPERATIONS ==========
    
    /**
     * Bulk update booking status.
     */
    @Modifying
    @Query("UPDATE Booking b SET b.status = :status WHERE b.id IN :ids")
    int bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * Cancel all bookings for a show.
     */
    @Modifying
    @Query("UPDATE Booking b SET b.status = 'CANCELLED' WHERE b.showPublicId = :showPublicId AND b.status = 'ISSUED'")
    int cancelAllBookingsForShow(@Param("showPublicId") String showPublicId);

    /**
     * Expire completed show bookings (mark as historical).
     */
    @Modifying
    @Query("UPDATE Booking b SET b.status = 'COMPLETED' WHERE b.status = 'ISSUED' AND b.showEndTime < CURRENT_TIMESTAMP")
    int expireCompletedShowBookings();

    // ========== TICKET MANAGEMENT ==========
    
    /**
     * Find bookings that need to be printed (low print count).
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'ISSUED' AND b.printCount = 0")
    List<Booking> findBookingsNeedingPrint();

    /**
     * Increment print count for a booking.
     */
    @Modifying
    @Query("UPDATE Booking b SET b.printCount = b.printCount + 1 WHERE b.id = :bookingId")
    int incrementPrintCount(@Param("bookingId") Long bookingId);

    // ========== ADDITIONAL QUERIES FOR SERVICE IMPL ==========
    
    /**
     * Find show seats by confirmed booking ID.
     */
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.confirmedBookingId = :bookingId")
    List<Object[]> findShowSeatsByBookingId(@Param("bookingId") Long bookingId);

    /**
     * Count total bookings.
     */
    long count();

    /**
     * Count bookings by status.
     */
    long countByStatus(String status);
    
    /**
     * Find Bookings by using bookingGroupRef
     */
    List<Booking> findByBookingGroupRef(String bookingGroupRef);

	boolean existsByShowPublicIdAndSeatPublicIdAndStatus(String showPublicId, String seatPublicId, String label);

    
}