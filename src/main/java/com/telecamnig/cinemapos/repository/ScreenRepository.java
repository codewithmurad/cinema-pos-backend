package com.telecamnig.cinemapos.repository;

import com.telecamnig.cinemapos.entity.Screen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {

    // ========== BASIC CRUD & FINDERS ==========
    
    /**
     * Find screen by publicId (for external API calls)
     */
    Optional<Screen> findByPublicId(String publicId);

    /**
     * Find screen by unique code (e.g., "AUD1", "VIP1")
     */
    Optional<Screen> findByCode(String code);

    /**
     * Check if screen with given code exists (for validation)
     */
    boolean existsByCode(String code);

    /**
     * Check if screen with given publicId exists
     */
    boolean existsByPublicId(String publicId);

    // ========== STATUS-BASED FILTERS ==========
    
    /**
     * Find all screens by status
     */
    List<Screen> findByStatus(int status);

    /**
     * Find all ACTIVE screens ordered by name
     */
    @Query("SELECT s FROM Screen s WHERE s.status = 1 ORDER BY s.name")
    List<Screen> findAllActiveScreens();

    /**
     * Find screens by status with pagination
     */
    Page<Screen> findByStatus(int status, Pageable pageable);

    /**
     * Find active screens by category
     */
    List<Screen> findByCategoryAndStatus(String category, int status);

    // ========== SEARCH & FILTER METHODS ==========
    
    /**
     * Search screens by name (case-insensitive partial match)
     */
    @Query("SELECT s FROM Screen s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) AND s.status = 1")
    List<Screen> findActiveByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Search screens by code or name
     */
    @Query("SELECT s FROM Screen s WHERE (LOWER(s.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND s.status = 1")
    List<Screen> searchActiveScreens(@Param("searchTerm") String searchTerm);

    // ========== BULK OPERATIONS ==========
    
    /**
     * Bulk update screen status
     */
    @Modifying
    @Query("UPDATE Screen s SET s.status = :status WHERE s.id IN :ids")
    int bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("status") int status);

    /**
     * Soft delete screen by publicId
     */
    @Modifying
    @Query("UPDATE Screen s SET s.status = 2 WHERE s.publicId = :publicId")
    int softDeleteByPublicId(@Param("publicId") String publicId);

    // ========== COUNT & AGGREGATION METHODS ==========
    
    /**
     * Count screens by status
     */
    long countByStatus(int status);

    /**
     * Count screens by category
     */
    @Query("SELECT s.category, COUNT(s) FROM Screen s WHERE s.status = 1 GROUP BY s.category")
    List<Object[]> countActiveScreensByCategory();

    // ========== VALIDATION METHODS ==========
    
    /**
     * Check if code exists excluding current screen (for updates)
     */
    @Query("SELECT COUNT(s) > 0 FROM Screen s WHERE s.code = :code AND s.publicId != :excludePublicId")
    boolean existsByCodeExcludingPublicId(@Param("code") String code, @Param("excludePublicId") String excludePublicId);

    /**
     * Check if name exists excluding current screen
     */
    @Query("SELECT COUNT(s) > 0 FROM Screen s WHERE LOWER(s.name) = LOWER(:name) AND s.publicId != :excludePublicId")
    boolean existsByNameExcludingPublicId(@Param("name") String name, @Param("excludePublicId") String excludePublicId);
}