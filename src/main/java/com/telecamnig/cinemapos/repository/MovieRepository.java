package com.telecamnig.cinemapos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telecamnig.cinemapos.entity.Movie;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    // ========== BASIC FINDERS ==========
    
    /**
     * Find movie by publicId (UUID).
     * Indexed: idx_movies_publicid
     */
    Optional<Movie> findByPublicId(String publicId);

    /**
     * Find movie by title (exact match, case-insensitive).
     */
    Optional<Movie> findByTitleIgnoreCase(String title);

    /**
     * Check if movie title exists (case-insensitive, for validation).
     */
    boolean existsByTitleIgnoreCase(String title);
    
 // Add this method back to your MovieRepository:

    /**
     * Search movies by title (partial match, case-insensitive) without status filter.
     * Used for admin/search functionality where status filtering is optional.
     */
    List<Movie> findByTitleContainingIgnoreCase(String title);

    // ========== STATUS-BASED FILTERS ==========
    
    /**
     * Find movies by status.
     * Indexed: idx_movies_status_release
     */
    List<Movie> findByStatus(int status);

    /**
     * Find active movies ordered by release date (newest first).
     */
    @Query("SELECT m FROM Movie m WHERE m.status = 1 ORDER BY m.releaseDate DESC")
    List<Movie> findActiveMoviesOrderByReleaseDateDesc();

    /**
     * Find upcoming movies (status = UPCOMING) ordered by release date.
     */
    @Query("SELECT m FROM Movie m WHERE m.status = 0 ORDER BY m.releaseDate ASC")
    List<Movie> findUpcomingMoviesOrderByReleaseDate();

    // ========== SEARCH & FILTER METHODS ==========
    
    /**
     * Search movies by title (partial match, case-insensitive) and status.
     */
    List<Movie> findByTitleContainingIgnoreCaseAndStatus(String title, int status);

    /**
     * Search movies by genre and status.
     */
    @Query("SELECT m FROM Movie m WHERE m.genres LIKE %:genre% AND m.status = :status")
    List<Movie> findByGenreContainingAndStatus(@Param("genre") String genre, @Param("status") int status);

    /**
     * Search movies by language and status.
     */
    List<Movie> findByLanguageIgnoreCaseAndStatus(String language, int status);

    /**
     * Find movies by format (3D/IMAX) and status.
     */
    List<Movie> findByIs3DAndStatus(Boolean is3D, int status);

    List<Movie> findByIsIMAXAndStatus(Boolean isIMAX, int status);
    
    // For better search performance with large catalogs
    @Query("SELECT m FROM Movie m WHERE m.status = :status AND " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.director) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.castMembers) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Movie> searchMovies(@Param("search") String search, @Param("status") int status, Pageable pageable);

    // ========== PAGINATION METHODS ==========
    
    /**
     * Find movies by status with pagination.
     */
    Page<Movie> findByStatus(int status, Pageable pageable);

    /**
     * Search movies by title and status with pagination.
     */
    Page<Movie> findByTitleContainingIgnoreCaseAndStatus(String title, int status, Pageable pageable);

    /**
     * Search all movies by title with pagination.
     */
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * Find active movies with pagination, ordered by release date.
     */
    @Query("SELECT m FROM Movie m WHERE m.status = 1 ORDER BY m.releaseDate DESC")
    Page<Movie> findActiveMoviesOrderByReleaseDateDesc(Pageable pageable);

    // ========== COUNT & AGGREGATION METHODS ==========
    
    /**
     * Count movies by status.
     */
    long countByStatus(int status);

    /**
     * Count movies by language.
     */
    @Query("SELECT m.language, COUNT(m) FROM Movie m WHERE m.status = 1 GROUP BY m.language")
    List<Object[]> countActiveMoviesByLanguage();

    /**
     * Count movies by format.
     */
    @Query("SELECT '3D', COUNT(m) FROM Movie m WHERE m.is3D = true AND m.status = 1 " +
           "UNION ALL " +
           "SELECT 'IMAX', COUNT(m) FROM Movie m WHERE m.isIMAX = true AND m.status = 1")
    List<Object[]> countActiveMoviesByFormat();

    // ========== VALIDATION METHODS ==========
    
    /**
     * Check if title exists excluding a specific movie (for updates).
     */
    @Query("SELECT COUNT(m) > 0 FROM Movie m WHERE LOWER(m.title) = LOWER(:title) AND m.publicId != :excludePublicId")
    boolean existsByTitleIgnoreCaseExcludingMovie(@Param("title") String title, 
                                                @Param("excludePublicId") String excludePublicId);

    /**
     * Check if publicId exists.
     */
    boolean existsByPublicId(String publicId);

    // ========== BUSINESS SPECIFIC QUERIES ==========
    
    /**
     * Find currently running movies (ACTIVE status) for show scheduling.
     */
    @Query("SELECT m FROM Movie m WHERE m.status = 1 AND m.releaseDate <= CURRENT_TIMESTAMP ORDER BY m.title")
    List<Movie> findCurrentlyRunningMovies();

    /**
     * Find movies eligible for scheduling (ACTIVE + release date passed).
     */
    @Query("SELECT m FROM Movie m WHERE m.status = 1 AND m.releaseDate <= CURRENT_TIMESTAMP")
    List<Movie> findMoviesEligibleForScheduling();

    /**
     * Find movies by parental rating and status.
     */
    List<Movie> findByParentalRatingAndStatus(String parentalRating, int status);
    
    // ========== Bulk Operations ==========
    
    // For admin batch operations
    @Modifying
    @Query("UPDATE Movie m SET m.status = :newStatus WHERE m.id IN :ids")
    int bulkUpdateMovieStatus(@Param("ids") List<Long> ids, @Param("newStatus") int newStatus);
    
    
}