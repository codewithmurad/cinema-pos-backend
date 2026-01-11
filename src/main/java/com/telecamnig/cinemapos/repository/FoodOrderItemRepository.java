package com.telecamnig.cinemapos.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telecamnig.cinemapos.entity.FoodOrderItem;

@Repository
public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem, Long> {

    // ==================== BILL / INVOICE ====================

    @Query("""
        SELECT foi FROM FoodOrderItem foi
        WHERE foi.orderId = :orderId
        ORDER BY foi.id ASC
        """)
    List<FoodOrderItem> findByOrderId(@Param("orderId") Long orderId);

    // ==================== REPORTING ====================

    @Query("""
        SELECT
            foi.foodName,
            SUM(foi.quantity),
            SUM(foi.lineTotal)
        FROM FoodOrderItem foi
        WHERE foi.createdAt BETWEEN :start AND :end
        GROUP BY foi.foodName
        ORDER BY SUM(foi.quantity) DESC
        """)
    List<Object[]> findFoodSalesSummary(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT foi FROM FoodOrderItem foi
        WHERE foi.foodId = :foodId
        ORDER BY foi.createdAt DESC
        """)
    List<FoodOrderItem> findByFoodId(@Param("foodId") Long foodId);
    
    /**
     * Get top selling food items for date range - SIMPLIFIED QUERY
     */
    @Query("""
        SELECT 
            foi.foodName as foodName,
            COALESCE(fc.name, 'Uncategorized') as categoryName,
            SUM(foi.quantity) as totalQuantity,
            SUM(foi.lineTotal) as totalAmount
        FROM FoodOrderItem foi
        JOIN FoodOrder fo ON fo.id = foi.orderId
        LEFT JOIN Food f ON f.id = foi.foodId
        LEFT JOIN FoodCategory fc ON fc.id = f.categoryId
        WHERE fo.status = :status
          AND (:startDate IS NULL OR fo.createdAt >= :startDate)
          AND (:endDate IS NULL OR fo.createdAt <= :endDate)
        GROUP BY foi.foodName, fc.name
        ORDER BY SUM(foi.quantity) DESC
        """)
    List<Object[]> getTopSellingItems(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") Integer status
    );

}