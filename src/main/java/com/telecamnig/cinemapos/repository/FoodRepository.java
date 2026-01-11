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

import com.telecamnig.cinemapos.entity.Food;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    // ==================== BASIC LOOKUPS ====================

    @Query("SELECT f FROM Food f WHERE f.publicId = :publicId")
    Optional<Food> findByPublicId(@Param("publicId") String publicId);

    @Query("SELECT COUNT(f) > 0 FROM Food f WHERE f.publicId = :publicId")
    boolean existsByPublicId(@Param("publicId") String publicId);

    @Query("""
        SELECT COUNT(f) > 0 FROM Food f
        WHERE LOWER(f.name) = LOWER(:name)
          AND f.categoryId = :categoryId
        """)
    boolean existsByNameAndCategory(
            @Param("name") String name,
            @Param("categoryId") Long categoryId
    );

    // ==================== FOOD COUNTER QUERIES ====================

    /**
     * Fetch foods for Food Counter (single category).
     * Only ACTIVE foods, sorted for fast UI rendering.
     */
    @Query("""
        SELECT f FROM Food f
        WHERE f.categoryId = :categoryId
          AND f.status = :status
        ORDER BY
          COALESCE(f.displayOrder, 9999) ASC,
          f.name ASC
        """)
    List<Food> findActiveFoodsByCategoryForCounter(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status
    );

    /**
     * Fetch all ACTIVE foods for Food Counter (all categories).
     * Useful for caching or preload.
     */
    @Query("""
        SELECT f FROM Food f
        WHERE f.status = :status
        ORDER BY
          f.categoryId ASC,
          COALESCE(f.displayOrder, 9999) ASC,
          f.name ASC
        """)
    List<Food> findAllActiveFoodsForCounter(
            @Param("status") Integer status
    );
    
    @Query("""
    	    SELECT f FROM Food f
    	    WHERE f.status = :status
    	      AND (:categoryId IS NULL OR f.categoryId = :categoryId)
    	      AND (:search IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')))
    	    ORDER BY 
    	      COALESCE(f.displayOrder, 9999) ASC,
    	      f.name ASC
    	    """)
    	List<Food> findFoodsForCounter(
    	        @Param("status") Integer status,
    	        @Param("categoryId") Long categoryId,
    	        @Param("search") String search
    	);

    // ==================== ADMIN PANEL QUERIES ====================

    /**
     * Fetch foods by category (Admin).
     */
    @Query("""
        SELECT f FROM Food f
        WHERE (:categoryId IS NULL OR f.categoryId = :categoryId)
          AND (:status IS NULL OR f.status = :status)
        ORDER BY f.createdAt DESC
        """)
    Page<Food> findFoodsForAdmin(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            Pageable pageable
    );

    /**
     * Search foods by name (Admin).
     */
    @Query("""
        SELECT f FROM Food f
        WHERE (:categoryId IS NULL OR f.categoryId = :categoryId)
          AND (:status IS NULL OR f.status = :status)
          AND (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY f.createdAt DESC
        """)
    Page<Food> searchFoods(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    
    @Query("""
    	    SELECT f FROM Food f
    	    WHERE f.status = :status
    	      AND (:categoryId IS NULL OR f.categoryId = :categoryId)
    	      AND (:search IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')))
    	    ORDER BY f.updatedAt DESC
    	    """)
    	List<Food> findByStatusForAdmin(
    	        @Param("status") Integer status,
    	        @Param("categoryId") Long categoryId,
    	        @Param("search") String search
    	);

    	@Query("""
    	    SELECT f FROM Food f
    	    WHERE f.status = :status
    	      AND (:categoryId IS NULL OR f.categoryId = :categoryId)
    	      AND (:search IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')))
    	    """)
    	Page<Food> findByStatusForAdminPaged(
    	        @Param("status") Integer status,
    	        @Param("categoryId") Long categoryId,
    	        @Param("search") String search,
    	        Pageable pageable
    	);

    	
    // ==================== STATUS / SOFT DELETE ====================

    /**
     * Update food status (soft delete / enable / disable).
     */
    @Modifying
    @Transactional
    @Query("UPDATE Food f SET f.status = :status WHERE f.id = :id")
    int updateStatus(
            @Param("id") Long id,
            @Param("status") Integer status
    );

    /**
     * Bulk status update.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Food f SET f.status = :status WHERE f.id IN :ids")
    int updateStatusBulk(
            @Param("ids") List<Long> ids,
            @Param("status") Integer status
    );

    // ==================== DASHBOARD / STATS ====================

    /**
     * Count foods grouped by status.
     */
    @Query("SELECT f.status, COUNT(f) FROM Food f GROUP BY f.status")
    List<Object[]> countByStatus();

    /**
     * Count foods per category (Admin analytics).
     */
    @Query("""
        SELECT f.categoryId, COUNT(f)
        FROM Food f
        WHERE f.status = :status
        GROUP BY f.categoryId
        """)
    List<Object[]> countActiveFoodsByCategory(@Param("status") Integer status);

	@Query("""
		    SELECT f FROM Food f
		    WHERE f.publicId IN :publicIds
		      AND f.status = :status
		""")
		List<Food> findByPublicIdInAndStatus(
		        @Param("publicIds") List<String> publicIds,
		        @Param("status") int status
		);


}
