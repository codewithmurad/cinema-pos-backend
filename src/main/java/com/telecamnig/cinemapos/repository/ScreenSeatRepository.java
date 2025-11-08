package com.telecamnig.cinemapos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telecamnig.cinemapos.entity.ScreenSeat;

@Repository
public interface ScreenSeatRepository extends JpaRepository<ScreenSeat, Long> {

	// ========== BASIC CRUD & FINDERS ==========

	/**
	 * Find seat by publicId (for external API calls)
	 */
	Optional<ScreenSeat> findByPublicId(String publicId);

	/**
	 * Find all seats for a specific screen
	 */
	List<ScreenSeat> findByScreenId(Long screenId);

	/**
	 * Find seats by screenId with pagination
	 */
	Page<ScreenSeat> findByScreenId(Long screenId, Pageable pageable);

	/**
	 * Find seat by screenId and label (for uniqueness validation)
	 */
	Optional<ScreenSeat> findByScreenIdAndLabel(Long screenId, String label);

	// ========== STATUS-BASED FILTERS ==========

	/**
	 * Find seats by screenId and status
	 */
	List<ScreenSeat> findByScreenIdAndStatus(Long screenId, int status);

	/**
	 * Find all ACTIVE seats for a screen ordered by row and column
	 */
	@Query("SELECT s FROM ScreenSeat s WHERE s.screenId = :screenId AND s.status = 1 ORDER BY s.rowIndex, s.colIndex")
	List<ScreenSeat> findActiveSeatsByScreenIdOrdered(@Param("screenId") Long screenId);

	/**
	 * Find seats by status
	 */
	List<ScreenSeat> findByStatus(int status);

	// ========== SEAT TYPE & GROUP FILTERS ==========

	/**
	 * Find seats by seat type
	 */
	List<ScreenSeat> findBySeatType(String seatType);

	/**
	 * Find seats by screenId and seat type
	 */
	List<ScreenSeat> findByScreenIdAndSeatType(Long screenId, String seatType);

	/**
	 * Find seats belonging to a specific group
	 */
	List<ScreenSeat> findByGroupPublicId(String groupPublicId);

	/**
	 * Find seats by screenId and group
	 */
	List<ScreenSeat> findByScreenIdAndGroupPublicId(Long screenId, String groupPublicId);

	// ========== POSITION-BASED FILTERS ==========

	/**
	 * Find seats by row index
	 */
	List<ScreenSeat> findByScreenIdAndRowIndex(Long screenId, Integer rowIndex);

	/**
	 * Find seats by column index
	 */
	List<ScreenSeat> findByScreenIdAndColIndex(Long screenId, Integer colIndex);

	/**
	 * Find seats within row range
	 */
	@Query("SELECT s FROM ScreenSeat s WHERE s.screenId = :screenId AND s.rowIndex BETWEEN :startRow AND :endRow AND s.status = 1")
	List<ScreenSeat> findSeatsByRowRange(@Param("screenId") Long screenId, @Param("startRow") Integer startRow,
			@Param("endRow") Integer endRow);

	// ========== BULK OPERATIONS ==========

	/**
	 * Bulk update seat status for a screen
	 */
	@Modifying
	@Query("UPDATE ScreenSeat s SET s.status = :status WHERE s.screenId = :screenId")
	int bulkUpdateStatusByScreenId(@Param("screenId") Long screenId, @Param("status") int status);

	/**
	 * Bulk update seat status by publicIds
	 */
	@Modifying
	@Query("UPDATE ScreenSeat s SET s.status = :status WHERE s.publicId IN :publicIds")
	int bulkUpdateStatusByPublicIds(@Param("publicIds") List<String> publicIds, @Param("status") int status);

	/**
	 * Soft delete all seats for a screen
	 */
	@Modifying
	@Query("UPDATE ScreenSeat s SET s.status = 2 WHERE s.screenId = :screenId")
	int softDeleteAllByScreenId(@Param("screenId") Long screenId);

	/**
	 * Delete all seats for a screen (hard delete - use with caution)
	 */
	@Modifying
	@Query("DELETE FROM ScreenSeat s WHERE s.screenId = :screenId")
	int deleteAllByScreenId(@Param("screenId") Long screenId);

	// ========== COUNT & AGGREGATION METHODS ==========

	/**
	 * Count seats by screenId
	 */
	long countByScreenId(Long screenId);

	/**
	 * Count active seats by screenId
	 */
	@Query("SELECT COUNT(s) FROM ScreenSeat s WHERE s.screenId = :screenId AND s.status = 1")
	long countActiveSeatsByScreenId(@Param("screenId") Long screenId);

	/**
	 * Count seats by screenId and status
	 */
	long countByScreenIdAndStatus(Long screenId, int status);

	/**
	 * Count seats by type for a screen
	 */
	@Query("SELECT s.seatType, COUNT(s) FROM ScreenSeat s WHERE s.screenId = :screenId AND s.status = 1 GROUP BY s.seatType")
	List<Object[]> countActiveSeatsByType(@Param("screenId") Long screenId);

	/**
	 * Get seat type distribution for a screen
	 */
	@Query("SELECT s.seatType, COUNT(s) FROM ScreenSeat s WHERE s.screenId = :screenId GROUP BY s.seatType")
	List<Object[]> getSeatTypeDistribution(@Param("screenId") Long screenId);

	// ========== VALIDATION METHODS ==========

	/**
	 * Check if label exists in screen (for uniqueness validation)
	 */
	boolean existsByScreenIdAndLabel(Long screenId, String label);

	/**
	 * Check if label exists excluding current seat (for updates)
	 */
	@Query("SELECT COUNT(s) > 0 FROM ScreenSeat s WHERE s.screenId = :screenId AND s.label = :label AND s.publicId != :excludePublicId")
	boolean existsByScreenIdAndLabelExcludingPublicId(@Param("screenId") Long screenId, @Param("label") String label,
			@Param("excludePublicId") String excludePublicId);

	/**
	 * Check if publicId exists
	 */
	boolean existsByPublicId(String publicId);

	// ========== BATCH OPERATIONS FOR SCREEN SETUP ==========

	/**
	 * Find all seats by screen publicId (convenience method)
	 */
	@Query("SELECT ss FROM ScreenSeat ss JOIN Screen s ON ss.screenId = s.id WHERE s.publicId = :screenPublicId")
	List<ScreenSeat> findByScreenPublicId(@Param("screenPublicId") String screenPublicId);

	/**
	 * Find active seats by screen publicId
	 */
	@Query("SELECT ss FROM ScreenSeat ss JOIN Screen s ON ss.screenId = s.id WHERE s.publicId = :screenPublicId AND ss.status = 1")
	List<ScreenSeat> findActiveSeatsByScreenPublicId(@Param("screenPublicId") String screenPublicId);

	// ========== ADVANCED QUERIES FOR FRONTEND ==========

	/**
	 * Get seat layout data for frontend rendering
	 */
	@Query("SELECT ss.publicId, ss.label, ss.seatType, ss.metaJson, ss.status, ss.groupPublicId "
			+ "FROM ScreenSeat ss WHERE ss.screenId = :screenId AND ss.status = 1")
	List<Object[]> getSeatLayoutData(@Param("screenId") Long screenId);

	/**
	 * Find seats with coordinates (for layout rendering)
	 */
	@Query("SELECT ss FROM ScreenSeat ss WHERE ss.screenId = :screenId AND ss.metaJson IS NOT NULL AND ss.status = 1")
	List<ScreenSeat> findSeatsWithLayoutData(@Param("screenId") Long screenId);

}