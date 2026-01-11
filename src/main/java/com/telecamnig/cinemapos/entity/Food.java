package com.telecamnig.cinemapos.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.telecamnig.cinemapos.utility.Constants.FoodStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Food entity.
 *
 * Represents a sellable food item in cinema POS.
 * Examples: Small Popcorn, Coke 500ml, Combo Pack.
 *
 * Image is stored on local Windows filesystem.
 * DB stores only relative image path.
 */
@Entity
@Table(
    name = "foods",
    indexes = {
        @Index(name = "idx_foods_publicid", columnList = "public_id"),
        @Index(name = "idx_foods_categoryid", columnList = "category_id"),
        @Index(name = "idx_foods_status", columnList = "status"),
        @Index(name = "idx_foods_name", columnList = "name")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_foods_public_id", columnNames = {"public_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable external UUID for frontend & APIs.
     */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    /**
     * DB-level reference to food_categories.id.
     * Resolved from categoryPublicId in service layer.
     */
    @NotNull
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    /**
     * Optional convenience field for API responses.
     */
    @Column(name = "category_public_id", length = 36)
    private String categoryPublicId;

    /**
     * Food display name.
     */
    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Optional description (admin only).
     */
    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    /**
     * Base selling price (without tax).
     */
    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Size(max = 500)
    @Column(length = 500)
    private String imagePath;

    /**
     * Sorting order inside category (Food Counter UI).
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Food availability status.
     *
     * FoodStatus:
     * 0 = INACTIVE
     * 1 = ACTIVE
     * 2 = DELETED
     */
    @Column(nullable = false)
    private int status;

    /* ------------------ Audit Fields ------------------ */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    /**
     * Optimistic locking.
     */
    @Version
    private Integer version;

    /* ------------------ Lifecycle Hooks ------------------ */

    @PrePersist
    protected void onCreate() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.status == 0) {
            this.status = FoodStatus.ACTIVE.getCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // intentionally empty
    }
}

