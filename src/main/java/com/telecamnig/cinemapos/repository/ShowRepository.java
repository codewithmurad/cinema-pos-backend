package com.telecamnig.cinemapos.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telecamnig.cinemapos.entity.Show;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    // ==================== BASIC CRUD WITH PUBLIC ID ====================
    
    @Query("SELECT s FROM Show s WHERE s.publicId = :publicId")
    Optional<Show> findByPublicId(@Param("publicId") String publicId);
    
    @Query("SELECT COUNT(s) > 0 FROM Show s WHERE s.publicId = :publicId")
    boolean existsByPublicId(@Param("publicId") String publicId);

    // ==================== TIME CONFLICT DETECTION ====================
    
    @Query("""
        SELECT s FROM Show s 
        WHERE s.screenId = :screenId 
        AND s.status IN (0, 1)
        AND (
            (s.startAt < :endAt AND s.endAt > :startAt) OR
            (s.startAt BETWEEN :startAt AND :endAt) OR
            (s.endAt BETWEEN :startAt AND :endAt)
        )
        ORDER BY s.startAt ASC
        """)
    List<Show> findConflictingShows(@Param("screenId") Long screenId, 
                                   @Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt);

    // ==================== SCREEN-BASED QUERIES ====================
    
    @Query("SELECT s FROM Show s WHERE s.screenId = :screenId ORDER BY s.startAt DESC")
    Page<Show> findByScreenId(@Param("screenId") Long screenId, Pageable pageable);
    
    @Query("SELECT s FROM Show s WHERE s.screenId = :screenId AND s.status IN (0, 1) ORDER BY s.startAt ASC")
    List<Show> findActiveShowsByScreenId(@Param("screenId") Long screenId);
    
    @Query("SELECT s FROM Show s WHERE s.screenId = :screenId AND s.startAt BETWEEN :startDate AND :endDate ORDER BY s.startAt ASC")
    List<Show> findByScreenIdAndDateRange(@Param("screenId") Long screenId, 
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // ==================== MOVIE-BASED QUERIES ====================
    
    @Query("SELECT s FROM Show s WHERE s.movieId = :movieId ORDER BY s.startAt DESC")
    Page<Show> findByMovieId(@Param("movieId") Long movieId, Pageable pageable);
    
    @Query("SELECT s FROM Show s WHERE s.movieId = :movieId AND s.status IN (0, 1) ORDER BY s.startAt ASC")
    List<Show> findActiveShowsByMovieId(@Param("movieId") Long movieId);

    // ==================== STATUS-BASED QUERIES ====================
    
    @Query("SELECT s FROM Show s WHERE s.status = :status ORDER BY s.startAt DESC")
    Page<Show> findByStatus(@Param("status") Integer status, Pageable pageable);
    
    @Query("SELECT s FROM Show s WHERE s.status = :status ORDER BY s.startAt DESC")
    List<Show> findByStatus(@Param("status") Integer status);

    // ==================== DATE RANGE QUERIES ====================
    
    @Query("SELECT s FROM Show s WHERE s.startAt BETWEEN :start AND :end ORDER BY s.startAt ASC")
    List<Show> findShowsByDateRange(@Param("start") LocalDateTime start, 
                                   @Param("end") LocalDateTime end);
    
    @Query("SELECT s FROM Show s WHERE s.startAt >= :startFrom AND s.status IN (0, 1) ORDER BY s.startAt ASC")
    List<Show> findUpcomingShows(@Param("startFrom") LocalDateTime startFrom);
    
    /**
     * Searches upcoming shows with optional filters.
     *
     * Behavior:
     * - Always filters by status and "from" datetime (startAt >= :from).
     * - If :to is non-null, also enforces startAt < :to (used for single-day search).
     * - If :movieId is non-null, filters by given movie.
     *
     * This allows:
     * - movie only     → from = now, to = null, movieId != null
     * - date only      → from/to set for that day, movieId = null
     * - movie + date   → from/to set for that day, movieId != null
     * - no filters     → from = now, to = null, movieId = null
     */
    @Query("""
        SELECT s FROM Show s
        WHERE s.status = :status
          AND s.startAt >= :from
          AND (:to IS NULL OR s.startAt < :to)
          AND (:movieId IS NULL OR s.movieId = :movieId)
        ORDER BY s.startAt ASC
        """)
    Page<Show> searchUpcomingShows(
            @Param("status") Integer status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("movieId") Long movieId,
            Pageable pageable
    );

    /**
     * Searches running shows with optional movie filter.
     * 
     * Business Rules:
     * - Must be in RUNNING status (status = 1)
     * - Current time must be between startAt (inclusive) and endAt (exclusive)
     * - If movieId is provided, filter by that movie
     * 
     * Ordering: Shows ending soonest first, then by screen
     */
    @Query("""
        SELECT s FROM Show s
        WHERE s.status = :status
          AND s.startAt <= :currentTime
          AND s.endAt > :currentTime
          AND (:movieId IS NULL OR s.movieId = :movieId)
        ORDER BY s.endAt ASC, s.screenId ASC
        """)
    Page<Show> searchRunningShows(
            @Param("status") Integer status,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("movieId") Long movieId,
            Pageable pageable
    );
    
    @Query("""
    	    SELECT s FROM Show s
    	    LEFT JOIN Movie m ON s.movieId = m.id
    	    WHERE s.status IN :statuses
    	      AND (
    	        (s.status = 0 AND s.startAt >= :currentTime)
    	        OR
    	        (s.status = 1 AND s.endAt >= :now)
    	      )
    	      AND (:movieId IS NULL OR s.movieId = :movieId)
    	      AND (:movieName IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :movieName, '%')))
    	      AND (:screenId IS NULL OR s.screenId = :screenId)
    	      AND (:showDate IS NULL OR DATE(s.startAt) = :showDate)
    	      AND (:startAfter IS NULL OR s.startAt >= :startAfter)
    	      AND (:endBefore IS NULL OR s.startAt <= :endBefore)
    	    ORDER BY s.startAt ASC
    	    """)
    	Page<Show> searchActiveShows(
    	        @Param("statuses") List<Integer> statuses,
    	        @Param("currentTime") LocalDateTime currentTime,
    	        @Param("now") LocalDateTime now,
    	        @Param("movieId") Long movieId,
    	        @Param("movieName") String movieName,
    	        @Param("screenId") Long screenId,
    	        @Param("showDate") LocalDate showDate,
    	        @Param("startAfter") LocalDateTime startAfter,
    	        @Param("endBefore") LocalDateTime endBefore,
    	        Pageable pageable
    	);
    
    /**
     * Searches historical shows with optional filters.
     * Consistent pattern with other search methods.
     * 
     * @param statuses List of status codes (COMPLETED, CANCELLED)
     * @param movieId Optional movie filter
     * @param startDateTime Optional start date filter (inclusive)
     * @param endDateTime Optional end date filter (exclusive)
     */
    @Query("""
        SELECT s FROM Show s
        WHERE s.status IN :statuses
          AND (:movieId IS NULL OR s.movieId = :movieId)
          AND (:startDateTime IS NULL OR s.startAt >= :startDateTime)
          AND (:endDateTime IS NULL OR s.startAt < :endDateTime)
        ORDER BY s.startAt DESC
        """)
    Page<Show> searchHistoryShows(
            @Param("statuses") List<Integer> statuses,
            @Param("movieId") Long movieId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable
    );

    // ==================== COMPLEX BUSINESS QUERIES ====================
    
    @Query("SELECT s FROM Show s WHERE s.status IN (0, 1) AND s.startAt >= :currentTime ORDER BY s.startAt ASC, s.screenId ASC")
    Page<Show> findActiveShowsForCounter(@Param("currentTime") LocalDateTime currentTime, Pageable pageable);
    
    /**
     * Finds SCHEDULED shows that have reached/passed their start time
     * (status = 0 AND startAt <= currentTime)
     * Used by scheduler to auto-update SCHEDULED → RUNNING
     */
    @Query("SELECT s FROM Show s WHERE s.status = 0 AND s.startAt <= :currentTime")
    List<Show> findScheduledShowsToStart(@Param("currentTime") LocalDateTime currentTime);
    
    // ==================== ADMIN DASHBOARD QUERIES ====================
    
    @Query("SELECT s.status, COUNT(s) FROM Show s GROUP BY s.status")
    List<Object[]> countShowsByStatus();
    
    @Query("SELECT s FROM Show s WHERE DATE(s.startAt) = CURRENT_DATE ORDER BY s.startAt ASC")
    List<Show> findTodaysShows();

    // ==================== BULK OPERATIONS ====================
    
    @Query("UPDATE Show s SET s.status = :newStatus WHERE s.id IN :showIds")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int updateShowStatusBulk(@Param("showIds") List<Long> showIds, @Param("newStatus") Integer newStatus);
    
    @Query("SELECT s FROM Show s WHERE s.status = :status AND s.startAt > :currentTime ORDER BY s.startAt ASC")
    Page<Show> findByStatusAndStartTimeAfter(@Param("status") Integer status, 
                                            @Param("currentTime") LocalDateTime currentTime,
                                            Pageable pageable);

    @Query("SELECT s FROM Show s WHERE s.status = 1 AND s.startAt <= :currentTime AND s.endAt > :currentTime ORDER BY s.screenId ASC")
    List<Show> findRunningShows(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT s FROM Show s WHERE s.status = 1 AND s.endAt <= :currentTime ORDER BY s.screenId ASC")
    List<Show> findRunningShowsThatHaveEnded(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT s FROM Show s WHERE s.status IN :statuses")
    Page<Show> findByStatusIn(@Param("statuses") List<Integer> statuses, Pageable pageable);

    @Query("SELECT s FROM Show s WHERE s.startAt BETWEEN :startDate AND :endDate")
    Page<Show> findByStartAtBetween(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate, 
                                   Pageable pageable);

}