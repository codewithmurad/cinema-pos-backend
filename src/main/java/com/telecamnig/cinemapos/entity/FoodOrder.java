package com.telecamnig.cinemapos.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.telecamnig.cinemapos.utility.Constants.FoodOrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FoodOrder entity.
 *
 * Represents a single food billing transaction in Cinema POS.
 * One FoodOrder = One printed bill / invoice.
 *
 * DESIGN NOTES:
 * - Stores VAT snapshot for historical accuracy
 * - Stores monetary totals only (no item-level data here)
 * - Optimized for reporting & offline POS usage
 */
@Entity
@Table(
    name = "food_orders",
    indexes = {
        @Index(name = "idx_food_orders_publicid", columnList = "public_id"),
        @Index(name = "idx_food_orders_bill_no", columnList = "bill_no"),
        @Index(name = "idx_food_orders_status", columnList = "status"),
        @Index(name = "idx_food_orders_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_food_orders_public_id", columnNames = {"public_id"}),
        @UniqueConstraint(name = "uq_food_orders_bill_no", columnNames = {"bill_no"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable external UUID used by APIs and frontend.
     */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    /**
     * Human-readable bill number (e.g. F-20240210-0001).
     * Used on printed receipts.
     */
    @Column(name = "bill_no", nullable = false, length = 50)
    private String billNo;

    /**
     * Total amount before VAT.
     */
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "sub_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal subTotal;

    /**
     * VAT percentage snapshot (e.g. 7.5).
     * Read from application.properties at billing time.
     */
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "vat_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatPercentage;

    /**
     * Calculated VAT amount.
     */
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount;

    /**
     * Final payable amount (subTotal + vatAmount).
     */
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;

    /**
     * Payment mode.
     * Example: CASH, CARD, UPI
     */
    @Column(name = "payment_mode", length = 20)
    private String paymentMode;

    /**
     * Order status.
     *
     * FoodOrderStatus:
     * 0 = CREATED
     * 1 = PAID
     * 2 = CANCELLED
     * 3 = REFUNDED
     */
    @Column(nullable = false)
    private int status;
    
    private Integer totalItems;

    /* ------------------ Audit Fields ------------------ */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    /**
     * Optimistic locking for concurrent POS operations.
     */
    @Version
    private Integer version;
    
    @Column(name = "print_count", nullable = false)
    private Integer printCount = 0;

    public void incrementPrintCount() {
        if (this.printCount == null) {
            this.printCount = 1;
        } else {
            this.printCount++;
        }
    }


    /* ------------------ Lifecycle Hooks ------------------ */
    
    @PrePersist
    protected void onCreate() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.status == 0) {
            this.status = FoodOrderStatus.PAID.getCode();
        }
    }


}

