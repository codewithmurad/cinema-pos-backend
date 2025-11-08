package com.telecamnig.cinemapos.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Movie entity (unchanged fields except for audit timestamps).
 *
 * createdAt and updatedAt are managed by Hibernate:
 * - @CreationTimestamp -> createdAt (set once on insert)
 * - @UpdateTimestamp   -> updatedAt (on update)
 */
@Entity
@Table(name = "movies",
       indexes = {
           @Index(name = "idx_movies_title", columnList = "title"),
           @Index(name = "idx_movies_status_release", columnList = "status, release_date"),
           @Index(name = "idx_movies_publicid", columnList = "public_id"),
           @Index(name = "idx_movies_created_at", columnList = "created_at")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_movies_public_id", columnNames = {"public_id"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // external UUID identifier (will be generated automatically if null)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Size(max = 5000)
    @Column
    private String description;

    @NotNull
    @Min(1)
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    // comma-separated genres, e.g. "Action,Adventure"
    @Size(max = 500)
    @Column(length = 500)
    private String genres;

    @Size(max = 100)
    @Column(length = 100)
    private String language;

    @Size(max = 100)
    @Column(length = 100)
    private String country;

    @Size(max = 250)
    @Column(length = 250)
    private String director;

    @Size(max = 2000)
    @Column(length = 2000)
    private String castMembers; // large text: "Actor A; Actor B; ..."

    @Size(max = 2000)
    @Column(length = 2000)
    private String producers;

    @Size(max = 250)
    @Column(length = 250)
    private String productionCompany;

    @Size(max = 250)
    @Column(length = 250)
    private String distributor;

    @Size(max = 500)
    @Column(length = 500)
    private String posterPath; // relative path stored by LocalStorageService

    @Column(name = "is_3d")
    private Boolean is3D = Boolean.FALSE;

    @Column(name = "is_imax")
    private Boolean isIMAX = Boolean.FALSE;

    @Column(name = "subtitles_available")
    private Boolean subtitlesAvailable = Boolean.FALSE;

    @Size(max = 500)
    @Column(length = 500)
    private String audioLanguages; // comma separated languages

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    @Column
    private Double rating = 0.0;

    @Size(max = 20)
    @Column(length = 20)
    private String parentalRating; // "G", "PG", "12", "15", "18", "18+"  Nigerian film ratings

    // scheduling dates and times (use LocalDateTime as requested)
    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    /* ------------- Audit fields (hibernate-managed) ------------- */

    /**
     * Creation timestamp managed by Hibernate. Non-updatable.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * ID of the user who created this row (optional)
     */
    @Column(name = "created_by", length = 100)
    private Long createdBy;

    /**
     * Updated timestamp managed by Hibernate.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private int status;   // from MovieStatus enum

    // optimistic locking
    @Version
    private Integer version;

    /* --- lifecycle hooks --- */

    /**
     * PrePersist used to ensure stable publicId and sensible defaults.
     * Avoid modifying timestamp fields here because @CreationTimestamp and @UpdateTimestamp
     * will be responsible for that.
     */
    @PrePersist
    protected void onCreate() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.rating == null) {
            this.rating = 0.0;
        }
        // do not set createdAt/updatedAt here — Hibernate will manage them
    }

    /**
     * PreUpdate left for consistency with existing pattern. Do not touch updatedAt here.
     */
    @PreUpdate
    protected void onUpdate() {
        if (this.rating == null) {
            this.rating = 0.0;
        }
    }
}
