package com.telecamnig.cinemapos.repository;

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

    // ==================== COMPLEX BUSINESS QUERIES ====================
    
    @Query("SELECT s FROM Show s WHERE s.status IN (0, 1) AND s.startAt >= :currentTime ORDER BY s.startAt ASC, s.screenId ASC")
    Page<Show> findActiveShowsForCounter(@Param("currentTime") LocalDateTime currentTime, Pageable pageable);
    
    @Query("SELECT s FROM Show s WHERE s.status IN (0, 1) AND s.startAt >= :currentTime ORDER BY s.startAt ASC, s.screenId ASC")
    List<Show> findShowsNeedingStatusUpdate(@Param("currentTime") LocalDateTime currentTime);

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

    @Query("SELECT s FROM Show s WHERE s.status IN :statuses")
    Page<Show> findByStatusIn(@Param("statuses") List<Integer> statuses, Pageable pageable);

    @Query("SELECT s FROM Show s WHERE s.startAt BETWEEN :startDate AND :endDate")
    Page<Show> findByStartAtBetween(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate, 
                                   Pageable pageable);
}