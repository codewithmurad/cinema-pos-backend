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
import org.springframework.transaction.annotation.Transactional;

import com.telecamnig.cinemapos.entity.FoodCategory;

@Repository
public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {

    // ==================== BASIC LOOKUPS ====================

    @Query("SELECT fc FROM FoodCategory fc WHERE fc.publicId = :publicId")
    Optional<FoodCategory> findByPublicId(@Param("publicId") String publicId);

    @Query("SELECT COUNT(fc) > 0 FROM FoodCategory fc WHERE fc.publicId = :publicId")
    boolean existsByPublicId(@Param("publicId") String publicId);

    @Query("SELECT COUNT(fc) > 0 FROM FoodCategory fc WHERE LOWER(fc.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    // ==================== POS COUNTER QUERIES ====================

    /**
     * Fetch categories for Food Counter.
     * Only ACTIVE categories, ordered for fast UI rendering.
     */
    @Query("""
        SELECT fc FROM FoodCategory fc
        WHERE fc.status = :status
        ORDER BY 
          COALESCE(fc.displayOrder, 9999) ASC,
          fc.name ASC
        """)
    List<FoodCategory> findActiveCategoriesForCounter(
            @Param("status") Integer status
    );

    // ==================== ADMIN QUERIES ====================

    /**
     * Fetch all categories with pagination (Admin panel).
     */
    @Query("""
        SELECT fc FROM FoodCategory fc
        WHERE (:status IS NULL OR fc.status = :status)
        ORDER BY fc.createdAt DESC
        """)
    Page<FoodCategory> findAllForAdmin(
            @Param("status") Integer status,
            Pageable pageable
    );

    /**
     * Search categories by name (Admin).
     */
    @Query("""
        SELECT fc FROM FoodCategory fc
        WHERE (:status IS NULL OR fc.status = :status)
          AND (:keyword IS NULL OR LOWER(fc.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY fc.createdAt DESC
        """)
    Page<FoodCategory> searchCategories(
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // ==================== BULK / STATUS OPERATIONS ====================

    /**
     * Soft delete category.
     */
    @Modifying
    @Transactional
    @Query("UPDATE FoodCategory fc SET fc.status = :status WHERE fc.id = :id")
    int updateStatus(
            @Param("id") Long id,
            @Param("status") Integer status
    );

    /**
     * Bulk status update (future use).
     */
    @Modifying
    @Transactional
    @Query("UPDATE FoodCategory fc SET fc.status = :status WHERE fc.id IN :ids")
    int updateStatusBulk(
            @Param("ids") List<Long> ids,
            @Param("status") Integer status
    );

    // ==================== DASHBOARD / STATS ====================

    /**
     * Count categories grouped by status.
     * Used for admin dashboard.
     */
    @Query("SELECT fc.status, COUNT(fc) FROM FoodCategory fc GROUP BY fc.status")
    List<Object[]> countByStatus();

}
