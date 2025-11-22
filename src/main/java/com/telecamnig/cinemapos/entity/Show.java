package com.telecamnig.cinemapos.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.telecamnig.cinemapos.utility.Constants.ShowStatus;

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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Show entity — scheduled screening of a movie on a specific screen.
 *
 * Design notes:
 * - Uses numeric PKs (movieId, screenId) for fast DB joins and integrity.
 * - Exposes publicId (UUID string) for external APIs / frontend.
 * - startAt/endAt are LocalDateTime to represent the show window.
 * - status is stored as an int code (use com.telecamnig.cinemapos.constants.ShowStatus to interpret).
 *
 * When creating a Show, backend must:
 *  - resolve screenId (from screenPublicId) and movieId (from moviePublicId)
 *  - create ShowSeat rows for all seats belonging to the screen (resolving seatType -> price map)
 */
@Entity
@Table(name = "shows",
       indexes = {
           @Index(name = "idx_shows_publicid", columnList = "public_id"),
           @Index(name = "idx_shows_screenid_startat", columnList = "screen_id, start_at"),
           @Index(name = "idx_shows_movieid_startat", columnList = "movie_id, start_at"),
           @Index(name = "idx_shows_status_startat", columnList = "status, start_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External stable UUID used by frontend and API clients.
     */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    /**
     * DB-level FK to movies.id for efficient joins.
     * Resolve from moviePublicId in the service layer when accepting frontend requests.
     */
    @NotNull
    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    /**
     * Keep moviePublicId as convenience (optional). Frontend might send/expect it.
     * Keeping it is helpful to display in API responses without extra joins.
     */
    @Column(name = "movie_public_id", length = 36)
    private String moviePublicId;

    /**
     * DB-level FK to screens.id for efficient joins.
     * Resolve from screenPublicId in the service layer when accepting frontend requests.
     */
    @NotNull
    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    /**
     * Convenience field for API clients.
     */
    @Column(name = "screen_public_id", length = 36)
    private String screenPublicId;
    
    /**
     * Store the screen layout snapshot (TEXT)
     */
    @Lob
    @Column(name = "layout_json", columnDefinition = "TEXT")
    private String layoutJson;

    /**
     * Show start and end times (inclusive start, exclusive end recommended).
     */
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    /**
     * Show lifecycle status (see ShowStatus). Stored as int code.
     * 0 = SCHEDULED, 1 = RUNNING, 2 = COMPLETED, 3 = CANCELLED
     */
    @Column(nullable = false)
    private int status;

    /* ------------------ Audit fields (hibernate-managed) ------------------ */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private Long createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Optimistic lock column.
     */
    @Version
    private Integer version;

    /* ------------------ Lifecycle hooks ------------------ */

    /**
     * Ensure publicId and sensible defaults are set before insert.
     */
    @PrePersist
    protected void onCreate() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        // default status to SCHEDULED if not set
        if (this.status == 0) {
            this.status = ShowStatus.SCHEDULED.getCode();
        }
    }

    /**
     * Reserved for validation on update. Do not alter timestamps here.
     */
    @PreUpdate
    protected void onUpdate() {
        // keep for possible validation rules
    }

    /* ------------------ Developer notes ------------------
     *
     * - When creating a Show from an API request:
     *    1. Resolve moviePublicId -> movieId using MovieRepository.findByPublicId(...)
     *    2. Resolve screenPublicId -> screenId using ScreenRepository.findByPublicId(...)
     *    3. Persist Show (with movieId & screenId)
     *    4. Create ShowSeat rows for each ScreenSeat of screenId (copy seatPublicId, seatType, price)
     *
     * - Use DB transaction when creating Show + ShowSeat rows to keep consistency.
     *
     * - Indexes: queries like findByScreenIdAndStartAtBetween() and findByPublicId() will be fast.
     *
     * ---------------------------------------------------------------- */
}
