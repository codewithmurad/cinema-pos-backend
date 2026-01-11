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

import com.telecamnig.cinemapos.entity.FoodOrder;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    // ==================== BASIC LOOKUPS ====================

    @Query("SELECT fo FROM FoodOrder fo WHERE fo.publicId = :publicId")
    Optional<FoodOrder> findByPublicId(@Param("publicId") String publicId);

    @Query("SELECT fo FROM FoodOrder fo WHERE fo.billNo = :billNo")
    Optional<FoodOrder> findByBillNo(@Param("billNo") String billNo);

    @Query("SELECT COUNT(fo) > 0 FROM FoodOrder fo WHERE fo.billNo = :billNo")
    boolean existsByBillNo(@Param("billNo") String billNo);

    // ==================== STATUS-BASED QUERIES ====================

    @Query("SELECT fo FROM FoodOrder fo WHERE fo.status = :status ORDER BY fo.createdAt DESC")
    Page<FoodOrder> findByStatus(
            @Param("status") Integer status,
            Pageable pageable
    );

    @Query("SELECT fo FROM FoodOrder fo WHERE fo.status IN :statuses ORDER BY fo.createdAt DESC")
    Page<FoodOrder> findByStatusIn(
            @Param("statuses") List<Integer> statuses,
            Pageable pageable
    );

    // ==================== DATE RANGE QUERIES ====================

    @Query("""
        SELECT fo FROM FoodOrder fo
        WHERE fo.createdAt BETWEEN :start AND :end
        ORDER BY fo.createdAt DESC
        """)
    List<FoodOrder> findOrdersBetweenDates(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT fo FROM FoodOrder fo
        WHERE fo.createdAt BETWEEN :start AND :end
          AND fo.status = :status
        ORDER BY fo.createdAt DESC
        """)
    List<FoodOrder> findOrdersBetweenDatesByStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") Integer status
    );

    // ==================== DASHBOARD QUERIES ====================

    /**
     * Used for daily sales dashboard.
     * Returns total amount grouped by status.
     */
    @Query("""
        SELECT fo.status, SUM(fo.grandTotal)
        FROM FoodOrder fo
        WHERE fo.createdAt BETWEEN :start AND :end
        GROUP BY fo.status
        """)
    List<Object[]> sumGrandTotalGroupedByStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Fetch latest food orders (counter/admin view).
     */
    @Query("""
        SELECT fo FROM FoodOrder fo
        ORDER BY fo.createdAt DESC
        """)
    Page<FoodOrder> findLatestOrders(Pageable pageable);
    
    @Query("""
            SELECT o FROM FoodOrder o
            WHERE (:fromDate IS NULL OR o.createdAt >= :fromDate)
              AND (:toDate IS NULL OR o.createdAt <= :toDate)
              AND (:status IS NULL OR o.status = :status)
            ORDER BY o.createdAt DESC
        """)
        Page<FoodOrder> findFoodOrdersByDateRange(
                @Param("fromDate") LocalDateTime fromDate,
                @Param("toDate") LocalDateTime toDate,
                @Param("status") Integer status,
                Pageable pageable
        );
    
    /**
     * Get dashboard summary for date range (PAID orders only)
     * FIXED: Properly cast array elements
     */
    @Query("""
        SELECT 
            COALESCE(SUM(fo.grandTotal), 0),
            COALESCE(SUM(fo.subTotal), 0),
            COALESCE(SUM(fo.vatAmount), 0),
            COUNT(fo.id)
        FROM FoodOrder fo
        WHERE fo.status = :status
          AND (:startDate IS NULL OR fo.createdAt >= :startDate)
          AND (:endDate IS NULL OR fo.createdAt <= :endDate)
        """)
    List<Object[]> getDashboardSummary(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") Integer status
    );

    /**
     * Get payment method breakdown for date range (PAID orders only)
     * FIXED: Return as List<Object[]>
     */
    @Query("""
        SELECT 
            fo.paymentMode,
            COALESCE(SUM(fo.subTotal), 0),
            COALESCE(SUM(fo.vatAmount), 0),
            COALESCE(SUM(fo.grandTotal), 0),
            COUNT(fo.id)
        FROM FoodOrder fo
        WHERE fo.status = :status
          AND (:startDate IS NULL OR fo.createdAt >= :startDate)
          AND (:endDate IS NULL OR fo.createdAt <= :endDate)
          AND fo.paymentMode IN ('CASH', 'POS')
        GROUP BY fo.paymentMode
        """)
    List<Object[]> getPaymentMethodBreakdown(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") Integer status
    );

    /**
     * Get hourly summary for today (for chart)
     */
    @Query("""
        SELECT 
            HOUR(fo.createdAt) as hour,
            COALESCE(SUM(fo.grandTotal), 0) as revenue,
            COUNT(fo.id) as orders
        FROM FoodOrder fo
        WHERE fo.status = 1
          AND DATE(fo.createdAt) = CURRENT_DATE
        GROUP BY HOUR(fo.createdAt)
        ORDER BY hour
        """)
    List<Object[]> getTodayHourlySummary();

}
