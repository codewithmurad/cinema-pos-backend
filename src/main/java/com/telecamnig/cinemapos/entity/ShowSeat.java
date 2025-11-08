package com.telecamnig.cinemapos.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.telecamnig.cinemapos.utility.Constants.ShowSeatState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ShowSeat entity — SNAPSHOT of seat state at show creation time.
 * 
 * CRITICAL DESIGN: This is a HISTORICAL SNAPSHOT, not a live reference to ScreenSeat.
 * When a show is created, we copy ALL relevant seat data from ScreenSeat to preserve 
 * the arrangement as it existed at that moment in time.
 *
 * This ensures:
 * - Historical ticket data remains valid even if ScreenSeat changes
 * - Seat layout for past shows doesn't change unexpectedly  
 * - Pricing and seat types are frozen at show creation time
 */
@Entity
@Table(name = "show_seats",
       indexes = {
           @Index(name = "idx_showseats_showid_state", columnList = "show_id, state"),
           @Index(name = "idx_showseats_publicid", columnList = "public_id"),
           @Index(name = "idx_showseats_seatpublicid", columnList = "seat_public_id")
       },
       uniqueConstraints = {
           // ensure one show-seat row per seat per show
           @UniqueConstraint(name = "uq_showseat_showid_seatpublicid", columnNames = {"show_id", "seat_public_id"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External stable identifier for this show-seat row (UUID).
     */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    /**
     * DB-level FK to shows.id.
     */
    @NotNull
    @Column(name = "show_id", nullable = false)
    private Long showId;

    /**
     * ORIGINAL ScreenSeat.publicId at time of show creation.
     * This is the ONLY reference back to the original seat.
     * We DON'T store screen_seat_id (Long) to avoid broken references if seats are deleted.
     */
    @Size(max = 36)
    @Column(name = "seat_public_id", nullable = false, length = 36)
    private String seatPublicId;

    // ========== HISTORICAL SNAPSHOT FIELDS ==========
    // These are COPIED from ScreenSeat at show creation time
    // and NEVER updated from ScreenSeat after that.

    /**
     * Seat label as it existed when show was created (e.g., "A1").
     * Preserved even if ScreenSeat.label changes later.
     */
    @Size(max = 50)
    @Column(name = "seat_label", nullable = false, length = 50)
    private String seatLabel;

    /**
     * Seat type at show creation time (REGULAR/PREMIUM/GOLD/SOFA/ACCESSIBLE).
     * Historical record - won't change even if ScreenSeat.seatType changes.
     */
    @Size(max = 50)
    @Column(name = "seat_type", nullable = false, length = 50)
    private String seatType;

    /**
     * Visual metadata SNAPSHOT at show creation time.
     * Preserves exact seat position and appearance for historical shows.
     */
    @Lob
    @Column(name = "layout_meta_json", columnDefinition = "TEXT")
    private String layoutMetaJson;

    /**
     * Row index at show creation time.
     */
    @Column(name = "row_index")
    private Integer rowIndex;

    /**
     * Column index at show creation time.  
     */
    @Column(name = "col_index")
    private Integer colIndex;

    /**
     * Group reference at show creation time.
     */
    @Size(max = 36)
    @Column(name = "group_public_id", length = 36)
    private String groupPublicId;

    // ========== RUNTIME FIELDS ==========

    /**
     * Resolved price for this seat at show-creation time. 
     * Frozen - won't change even if pricing strategy changes later.
     */
    @NotNull
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    /**
     * Runtime state (AVAILABLE | HELD | SOLD). 
     */
    @Size(max = 20)
    @Column(name = "state", length = 20, nullable = false)
    private String state;

    /**
     * Reference to the confirmed booking (Booking.id).
     * Use Long for fast DB joins and queries.
     * For frontend, we'll create DTOs with bookingPublicId.
     */
    @Column(name = "confirmed_booking_id")
    private Long confirmedBookingId;

    /* ------------------ Audit fields ------------------ */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private Long createdBy;

    /**
     * User who reserved this seat (nullable).
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;@Column(name = "reserved_by", length = 100)
    private String reservedBy;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Version
    private Integer version;

    /* ------------------ Lifecycle hooks ------------------ */

    @PrePersist
    protected void onCreate() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.state == null || this.state.isBlank()) {
            this.state = ShowSeatState.AVAILABLE.getLabel();
        }
        if (this.price == null) {
            this.price = BigDecimal.ZERO;
        }
        if (this.price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }

    /* ------------------ Business Logic Methods ------------------ */

    /**
     * Check if seat is available for booking.
     */
    public boolean isAvailable() {
        return ShowSeatState.AVAILABLE.getLabel().equals(this.state);
    }

    /**
     * Check if seat is held by a specific user.
     */
    public boolean isHeldBy(String userPublicId) {
        return ShowSeatState.HELD.getLabel().equals(this.state) 
                && userPublicId.equals(this.reservedBy);
    }

    /**
     * Check if hold has expired.
     */
    public boolean isHoldExpired() {
        return ShowSeatState.HELD.getLabel().equals(this.state) 
                && this.expiresAt != null 
                && LocalDateTime.now().isAfter(this.expiresAt);
    }
}