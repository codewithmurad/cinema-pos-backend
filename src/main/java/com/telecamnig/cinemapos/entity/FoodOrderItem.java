package com.telecamnig.cinemapos.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FoodOrderItem entity.
 *
 * Represents a single food line item inside a FoodOrder.
 *
 * IMPORTANT:
 * - Stores SNAPSHOT of food name & price at time of sale
 * - Required for accurate reporting and audit
 * - Never depends on current Food table values
 */
@Entity
@Table(
    name = "food_order_items",
    indexes = {
        @Index(name = "idx_food_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_food_order_items_food_id", columnList = "food_id"),
        @Index(name = "idx_food_order_items_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent FoodOrder reference.
     */
    @NotNull
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * Internal Food reference (for joins & analytics).
     */
    @NotNull
    @Column(name = "food_id", nullable = false)
    private Long foodId;

    /**
     * External Food reference for API responses.
     */
    @Column(name = "food_public_id", length = 36)
    private String foodPublicId;

    /**
     * Food name snapshot at time of billing.
     */
    @NotBlank
    @Column(name = "food_name", nullable = false, length = 150)
    private String foodName;

    /**
     * Unit selling price snapshot (without VAT).
     */
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Quantity sold.
     */
    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Line total = unitPrice × quantity.
     */
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    /* ------------------ Audit Fields ------------------ */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optimistic locking (rare but safe).
     */
    @Version
    private Integer version;

}
