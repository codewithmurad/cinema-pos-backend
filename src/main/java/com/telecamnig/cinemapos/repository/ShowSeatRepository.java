package com.telecamnig.cinemapos.repository;

import com.telecamnig.cinemapos.entity.ShowSeat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

	// ==================== BASIC CRUD WITH PUBLIC ID ====================

	/**
	 * Find ShowSeat by publicId Uses idx_showseats_publicid index
	 */
	@Query("SELECT ss FROM ShowSeat ss WHERE ss.publicId = :publicId")
	Optional<ShowSeat> findByPublicId(@Param("publicId") String publicId);

	/**
	 * Check if ShowSeat exists by publicId
	 */
	@Query("SELECT COUNT(ss) > 0 FROM ShowSeat ss WHERE ss.publicId = :publicId")
	boolean existsByPublicId(@Param("publicId") String publicId);

	// ==================== SHOW-BASED QUERIES ====================

	/**
	 * PRODUCTION OPTIMIZED: Find all seats for a show with state Uses
	 * idx_showseats_showid_state composite index
	 */
	@Query("SELECT ss FROM ShowSeat ss WHERE ss.showId = :showId AND ss.state = :state ORDER BY ss.rowIndex, ss.colIndex")
	List<ShowSeat> findByShowIdAndState(@Param("showId") Long showId, @Param("state") String state);

	/**
	 * Find all seats for a show (for seat map rendering)
	 */
	@Query("SELECT ss FROM ShowSeat ss WHERE ss.showId = :showId ORDER BY ss.rowIndex, ss.colIndex")
	List<ShowSeat> findByShowId(@Param("showId") Long showId);

	/**
	 * Count available seats for a show
	 */
	@Query("SELECT COUNT(ss) FROM ShowSeat ss WHERE ss.showId = :showId AND ss.state = 'AVAILABLE'")
	long countAvailableSeatsByShowId(@Param("showId") Long showId);

	/**
	 * Count seats by state for a show
	 */
	@Query("SELECT ss.state, COUNT(ss) FROM ShowSeat ss WHERE ss.showId = :showId GROUP BY ss.state")
	List<Object[]> countSeatsByState(@Param("showId") Long showId);

	// ==================== SEAT SELECTION & BOOKING QUERIES ====================

	/**
	 * Find available seats for booking with pagination
	 */
	@Query("SELECT ss FROM ShowSeat ss WHERE ss.showId = :showId AND ss.state = 'AVAILABLE' ORDER BY ss.rowIndex, ss.colIndex")
	Page<ShowSeat> findAvailableSeats(@Param("showId") Long showId, Pageable pageable);

	/**
	 * Find specific seats by publicIds for a show (seat selection)
	 */
	@Query("SELECT ss FROM ShowSeat ss WHERE ss.showId = :showId AND ss.seatPublicId IN :seatPublicIds")
	List<ShowSeat> findByShowIdAndSeatPublicIds(@Param("showId") Long showId,
			@Param("seatPublicIds") List<String> seatPublicIds);

	/**
	 * Find held seats that have expired (for cleanup job)
	 */
	@Query("SELECT ss FROM ShowSeat ss WHERE ss.state = 'HELD' AND ss.expiresAt < :currentTime")
	List<ShowSeat> findExpiredHeldSeats(@Param("currentTime") LocalDateTime currentTime);

	/**
	 * Find seats held by a specific user
	 */
	@Query("SELECT ss FROM ShowSeat ss WHERE ss.state = 'HELD' AND ss.reservedBy = :userId")
	List<ShowSeat> findSeatsHeldByUser(@Param("userId") String userId);

	// ==================== SOFA/GROUP SEAT QUERIES ====================

	/**
	 * Find all seats in a sofa group
	 */
	@Query("SELECT ss FROM ShowSeat ss WHERE ss.showId = :showId AND ss.groupPublicId = :groupPublicId ORDER BY ss.colIndex")
	List<ShowSeat> findByShowIdAndGroup(@Param("showId") Long showId, @Param("groupPublicId") String groupPublicId);

	/**
	 * Check if all seats in a group are available
	 */
	@Query("SELECT COUNT(ss) = 0 FROM ShowSeat ss WHERE ss.showId = :showId AND ss.groupPublicId = :groupPublicId AND ss.state != 'AVAILABLE'")
	boolean isGroupFullyAvailable(@Param("showId") Long showId, @Param("groupPublicId") String groupPublicId);

	// ==================== BULK OPERATIONS FOR BOOKING ====================

	/**
	 * Update seat states in bulk (for booking confirmation)
	 */
	@Query("UPDATE ShowSeat ss SET ss.state = :newState, ss.reservedBy = :userId, ss.reservedAt = :reservedAt, ss.expiresAt = :expiresAt WHERE ss.id IN :seatIds")
	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.transaction.annotation.Transactional
	int updateSeatStatesBulk(@Param("seatIds") List<Long> seatIds, @Param("newState") String newState,
			@Param("userId") String userId, @Param("reservedAt") LocalDateTime reservedAt,
			@Param("expiresAt") LocalDateTime expiresAt);

	/**
	 * Release held seats in bulk (for expired reservations)
	 */
	@Query("UPDATE ShowSeat ss SET ss.state = 'AVAILABLE', ss.reservedBy = NULL, ss.reservedAt = NULL, ss.expiresAt = NULL WHERE ss.id IN :seatIds")
	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.transaction.annotation.Transactional
	int releaseSeatsBulk(@Param("seatIds") List<Long> seatIds);

	/**
	 * Confirm booking for seats (HELD → SOLD)
	 */
	@Query("UPDATE ShowSeat ss SET ss.state = 'SOLD', ss.confirmedBookingId = :bookingId WHERE ss.id IN :seatIds AND ss.state = 'HELD'")
	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.transaction.annotation.Transactional
	int confirmBookingForSeats(@Param("seatIds") List<Long> seatIds, @Param("bookingId") Long bookingId);

	// ==================== ANALYTICS & REPORTING QUERIES ====================

	/**
	 * Get seat occupancy rate for a show
	 */
	@Query("SELECT COUNT(ss) * 100.0 / (SELECT COUNT(ss2) FROM ShowSeat ss2 WHERE ss2.showId = :showId) FROM ShowSeat ss WHERE ss.showId = :showId AND ss.state = 'SOLD'")
	Double getOccupancyRate(@Param("showId") Long showId);

	/**
	 * Find most popular seat types by bookings
	 */
	@Query("SELECT ss.seatType, COUNT(ss) FROM ShowSeat ss WHERE ss.showId = :showId AND ss.state = 'SOLD' GROUP BY ss.seatType ORDER BY COUNT(ss) DESC")
	List<Object[]> getPopularSeatTypes(@Param("showId") Long showId);

	// ==================== INTEGRITY CHECKS ====================

	/**
	 * Check if seats are available for booking (concurrency check)
	 */
	@Query("SELECT COUNT(ss) = :seatCount FROM ShowSeat ss WHERE ss.showId = :showId AND ss.seatPublicId IN :seatPublicIds AND ss.state = 'AVAILABLE'")
	boolean areSeatsAvailable(@Param("showId") Long showId, @Param("seatPublicIds") List<String> seatPublicIds,
			@Param("seatCount") int seatCount);

	/**
	 * Find duplicate seat assignments (integrity check)
	 */
	@Query("SELECT ss.seatPublicId, COUNT(ss) FROM ShowSeat ss WHERE ss.showId = :showId GROUP BY ss.seatPublicId HAVING COUNT(ss) > 1")
	List<Object[]> findDuplicateSeatAssignments(@Param("showId") Long showId);
	
	/**
	 * Count seats by show ID and state
	 */
	@Query("SELECT COUNT(ss) FROM ShowSeat ss WHERE ss.showId = :showId AND ss.state = :state")
	long countByShowIdAndState(@Param("showId") Long showId, @Param("state") String state);
	
	 /**
     * Find all show seats by confirmed booking ID
     */
    List<ShowSeat> findByConfirmedBookingId(Long confirmedBookingId);

}