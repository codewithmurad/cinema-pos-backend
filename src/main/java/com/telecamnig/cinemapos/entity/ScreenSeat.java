package com.telecamnig.cinemapos.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.telecamnig.cinemapos.utility.Constants.SeatStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
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
 * ScreenSeat (Seat) entity — master record for a physical seat in a Screen.
 *
 * DESIGN DECISIONS
 * - Uses DB numeric primary key (Screen.id) reference via {@code screenId} (Long) for fast
 *   database joins and indexing. This is the authoritative DB-level relationship.
 * - No JPA entity mapping (@ManyToOne) to Screen is present intentionally:
 *     -> Avoids accidental lazy loads, N+1 problems, and cascade side-effects.
 *     -> Keeps the backend service code explicit: resolve screen PK when needed.
 * - Frontend and external APIs use {@code publicId} (UUID string) as stable external identifier.
 *
 * TIMESTAMPS
 * - Uses Hibernate annotations for audit timestamps:
 *     @CreationTimestamp -> createdAt (set once on insert)
 *     @UpdateTimestamp   -> updatedAt (set automatically on update)
 *
 * JSON/LOB FIELDS
 * - {@code metaJson} stores arbitrary per-seat visual metadata (x,y,w,h,rotation,icon,...)
 *   as TEXT/Lob. If you migrate to Postgres in future, consider jsonb and an appropriate converter.
 */
@Entity
@Table(
    name = "screen_seats",
    indexes = {
        // index on FK for efficient joins and filtering by screen
        @Index(name = "idx_screen_seats_screen_id", columnList = "screen_id"),
        @Index(name = "idx_screen_seats_publicid", columnList = "public_id"),
        @Index(name = "idx_screen_seats_label", columnList = "label")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_screen_seats_public_id", columnNames = {"public_id"}),
        // Enforce seat label uniqueness within a screen at DB level
        @UniqueConstraint(name = "uq_screen_seat_label_per_screen", columnNames = {"screen_id", "label"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenSeat {

    /**
     * Internal DB primary key.
     * Use this for internal relations and joins; external clients should use publicId.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable external identifier for a seat (seatPublicId). Used by frontend and external APIs.
     * Auto-generated as UUID v4 if not provided.
     */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    /**
     * DB-level foreign key referencing screens.id (primary key).
     * Use this numeric FK for fast queries and joins in repository methods (findByScreenId...).
     *
     * Backend workflow:
     * - Frontend sends screenPublicId (UUID string) in API requests.
     * - Server resolves to Screen (screenRepository.findByPublicId(...)) and uses screen.getId()
     *   to set this field before persisting ScreenSeat.
     */
    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    /**
     * Visible seat label shown to customers (e.g., "A1"). Uniqueness is enforced per screen.
     */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String label;

    /**
     * Optional numeric row index for deterministic ordering when layoutJson is absent.
     * Useful for fallback rendering or CSV exports.
     */
    @Column(name = "row_index")
    private Integer rowIndex;

    /**
     * Optional numeric column index for deterministic ordering when layoutJson is absent.
     */
    @Column(name = "col_index")
    private Integer colIndex;

    /**
     * Logical seat type used to resolve pricing at show-creation time.
     * Examples: REGULAR, PREMIUM, GOLD, SOFA, ACCESSIBLE.
     *
     * Stored as String for flexibility. If types are stable, consider enum mapping later.
     */
    @Size(max = 50)
    @Column(name = "seat_type", length = 50)
    private String seatType;

    /**
     * Optional group id when seat belongs to a sofa / must-sell-together group.
     * Stored as a string UUID referencing the logical group defined in Screen.layoutJson or SeatGroup.
     */
    @Size(max = 36)
    @Column(name = "group_public_id", length = 36)
    private String groupPublicId;

    /**
     * Per-seat visual metadata as JSON text.
     * Example:
     * {
     *   "x": 120,
     *   "y": 200,
     *   "w": 36,
     *   "h": 36,
     *   "rotation": 0,
     *   "icon": "seat"
     * }
     *
     * Stored as TEXT/Lob. Consider jsonb migration when using Postgres.
     */
    @Lob
    @Column(name = "meta_json", columnDefinition = "TEXT")
    private String metaJson;

    /**
     * Status code for seat lifecycle:
     * 0 = INACTIVE, 1 = ACTIVE, 2 = DELETED (soft-delete).
     * Keep consistent with com.telecamnig.cinemapos.constants.ScreenStatus
     * or create a dedicated SeatStatus enum if you prefer separate semantics.
     */
    @Column(nullable = false)
    private int status;

    /* ------------------ Audit fields (hibernate-managed) ------------------ */

    /**
     * Created timestamp — set by Hibernate at insert time. Non-updatable.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * ID of the user who created this row (optional). Keep as Long for consistency with other entities.
     * If you use string-based userPublicId in other parts, we can change it later.
     */
    @Column(name = "created_by", length = 100)
    private Long createdBy;

    /**
     * Updated timestamp — set by Hibernate on update.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Optimistic lock version to detect concurrent updates.
     */
    @Version
    private Integer version;

    /* ------------------ Lifecycle hooks ------------------ */

    /**
     * PrePersist initialization:
     * - Ensure a stable publicId (UUID v4) is generated if not set by caller.
     * - Default status to ACTIVE if caller left it zero/unset.
     *
     * Timestamps are NOT set here because Hibernate's @CreationTimestamp/@UpdateTimestamp handle them.
     */
    @PrePersist
    protected void onCreate() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        // default to ACTIVE if not explicitly set
        if (this.status == 0) {
            this.status = SeatStatus.ACTIVE.getCode();
        }
    }

    /**
     * PreUpdate reserved for validations or invariant checks.
     * Avoid setting updatedAt here (handled by Hibernate).
     */
    @PreUpdate
    protected void onUpdate() {
        // add validations if required in future
    }

    /* ------------------ Developer notes / API contract ------------------
     *
     * - Querying pattern (recommended):
     *     Screen screen = screenRepository.findByPublicId(screenPublicId);
     *     List<ScreenSeat> seats = screenSeatRepository.findByScreenIdAndStatus(screen.getId(), ACTIVE);
     *
     * - When creating a seat from frontend:
     *     Accept screenPublicId in request DTO -> resolve to screenId (Long) on server ->
     *     set seat.screenId = screenId -> save seat.
     *
     * - Keep publicId immutable after creation. If you need to rename a seat label,
     *   update the label field, do NOT change publicId — ShowSeat and Tickets may reference it.
     *
     * - If you add DB-level foreign key constraint and NOT NULL on screen_id, ensure
     *   migration backfill is completed before enabling NOT NULL/FOREIGN KEY.
     *
     * ------------------------------------------------------------------ */
}


/**
 * Per-seat visual metadata as JSON text.
 * This stores the exact position, size, and appearance of each seat
 * for frontend rendering. Works together with Screen.layoutJson:
 * 
 * - Screen.layoutJson = Cinema hall blueprint (walls, aisles, screen position)
 * - ScreenSeat.metaJson = Individual seat coordinates and styling
 * 
 * Example structure:
 * {
 *   "x": 120,           // Horizontal position from left (pixels)
 *   "y": 200,           // Vertical position from top (pixels)
 *   "w": 36,            // Seat width (pixels)
 *   "h": 36,            // Seat height (pixels)
 *   "rotation": 0,      // Rotation angle in degrees (0 = straight)
 *   "type": "REGULAR",  // Seat type for CSS styling
 *   "icon": "seat"      // Optional icon for special seats
 * }
 * 
 * FRONTEND USAGE:
 * The frontend uses these coordinates to position each seat absolutely
 * within the canvas defined by Screen.layoutJson.
 * 
 * Example for 3 seats in a row:
 * Seat A1: {"x": 100, "y": 100, "w": 40, "h": 40, "type": "REGULAR"}
 * Seat A2: {"x": 150, "y": 100, "w": 40, "h": 40, "type": "REGULAR"}
 * Seat A3: {"x": 200, "y": 100, "w": 40, "h": 40, "type": "PREMIUM"}
 * 
 * Renders as:
 *     ┌─────────────────────┐
 *     │       SCREEN        │
 *     └─────────────────────┘
 *       100px  150px  200px
 *         ↓      ↓      ↓
 *     [ A1 ]  [ A2 ]  [ A3 ]   ← All at y=100px (same row)
 *     REGULAR REGULAR PREMIUM
 * 
 * Stored as TEXT/Lob. Consider jsonb migration when using Postgres.
 * 
 * @Lob
 * @Column(name = "meta_json", columnDefinition = "TEXT")
 * private String metaJson;
 * 
 */
