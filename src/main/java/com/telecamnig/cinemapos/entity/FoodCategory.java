package com.telecamnig.cinemapos.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.telecamnig.cinemapos.utility.Constants.FoodCategoryStatus;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FoodCategory entity.
 *
 * Master table for food grouping used in cinema food POS.
 * Examples: Popcorn, Beverages, Combos, Snacks.
 *
 * Design principles:
 * - int-based status for performance
 * - UUID publicId for frontend & APIs
 * - soft delete only (no hard delete)
 * - optimized for fast POS reads
 */
@Entity
@Table(
    name = "food_categories",
    indexes = {
        @Index(name = "idx_food_categories_publicid", columnList = "public_id"),
        @Index(name = "idx_food_categories_name", columnList = "name"),
        @Index(name = "idx_food_categories_status", columnList = "status"),
        @Index(name = "idx_food_categories_display_order", columnList = "display_order")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_food_categories_public_id", columnNames = {"public_id"}),
        @UniqueConstraint(name = "uq_food_categories_name", columnNames = {"name"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable external UUID for frontend and API usage.
     */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    /**
     * Category display name (unique).
     * Example: Popcorn, Beverages.
     */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Optional description (admin reference only).
     */
    @Size(max = 500)
    @Column(length = 500)
    private String description;

    /**
     * Order in which category appears on Food Counter UI.
     * Lower value = higher priority.
     */
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Size(max = 500)
    @Column(length = 500)
    private String imagePath;  // relative path stored by LocalStorageService

    /**
     * Category status (stored as int for performance).
     *
     * FoodCategoryStatus:
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
     * Optimistic locking for concurrent admin edits.
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
            this.status = FoodCategoryStatus.ACTIVE.getCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // intentionally empty – Hibernate manages timestamps
    }
}

