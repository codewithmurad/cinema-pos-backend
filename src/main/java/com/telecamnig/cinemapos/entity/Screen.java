package com.telecamnig.cinemapos.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.telecamnig.cinemapos.utility.Constants.ScreenStatus;

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
 * Screen (auditorium) entity.
 *
 * Timestamp fields use Hibernate annotations:
 *  - @CreationTimestamp -> createdAt (set once on insert)
 *  - @UpdateTimestamp   -> updatedAt (updated automatically on update)
 *
 * Note: We still keep a @PrePersist hook for publicId generation and other defaults.
 */
@Entity
@Table(
    name = "screens",
    indexes = {
        @Index(name = "idx_screens_code", columnList = "code"),
        @Index(name = "idx_screens_publicid", columnList = "public_id"),
        @Index(name = "idx_screens_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_screens_public_id", columnNames = {"public_id"}),
        @UniqueConstraint(name = "uq_screens_code", columnNames = {"code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External stable identifier (UUID v4). This is the value the frontend and external APIs
     * will use to refer to a screen. Automatically generated if not provided.
     */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String code; // short unique code, e.g. R1, VIP1

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 50)
    @Column(length = 50)
    private String category;

    /**
     * Screen layout configuration stored as JSON.
     * This is the master blueprint for the cinema hall layout - defines the overall
     * structure, dimensions, aisles, rows, and metadata. Individual seat positions
     * are stored in ScreenSeat.metaJson, not here.
     * 
     * The frontend uses this JSON to:
     * 1. Create the appropriate sized canvas
     * 2. Draw structural elements (screen, aisles, row labels)
     * 3. Understand the overall seating organization
     * 
     * Actual seat drawing is done using ScreenSeat.metaJson coordinates.
     */
    @Lob
    @Column(name = "layout_json", columnDefinition = "TEXT")
    private String layoutJson;

    /**
     * Use int `status` instead of boolean isActive.
     * Codes per ScreenStatus enum:
     * 0 = INACTIVE, 1 = ACTIVE, 2 = DELETED
     */
    @Column(nullable = false)
    private int status;

    /* ------------- Audit Fields (hibernate-managed) ------------- */

    /**
     * Set by Hibernate on insert. Marked non-updatable so application cannot accidentally overwrite it.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Set/updated by Hibernate on entity update.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private Long createdBy;

    @Version
    private Integer version;

    /* ---------------- Lifecycle hooks (retain non-timestamp init) ---------------- */

    /**
     * Keep PrePersist for non-timestamp initialization:
     * - ensure publicId exists
     * - default status to ACTIVE if not set
     *
     * Do NOT set timestamps here because Hibernate @CreationTimestamp/@UpdateTimestamp
     * will manage them.
     */
    @PrePersist
    protected void onCreate() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.status == 0) {
            // default to ACTIVE if caller hasn't set a meaningful value
            this.status = ScreenStatus.ACTIVE.getCode();
        }
    }

    /**
     * PreUpdate kept only for potential future needs (e.g., validation).
     * Avoid setting updatedAt here — Hibernate handles it.
     */
    @PreUpdate
    protected void onUpdate() {
        // Intentionally left blank to avoid conflicting with @UpdateTimestamp.
    }

    /* ---------------- Developer notes ----------------
     * - These timestamp annotations rely on Hibernate. If you run with another JPA provider,
     *   they will be ignored and timestamps won't be populated automatically.
     *
     * - If you switch to DB-side default timestamps instead (e.g., DEFAULT now()), make sure to
     *   adapt entity mapping and tests accordingly.
     *
     * -------------------------------------------------------------------- */
}


/**
 * Screen (auditorium) entity.
 * 
 * DESIGN OVERVIEW:
 * This entity represents a physical cinema screen/auditorium. Each screen has its own
 * seating layout and configuration. The system supports:
 * - 4 Normal screens with traditional row-column seating
 * - 2 VIP screens with sofa group seating
 * - Different categories: REGULAR, PREMIUM, GOLD, VIP
 * 
 * LAYOUT MANAGEMENT SYSTEM:
 * 
 * 1. SCREEN LAYOUT (layoutJson) - THE CINEMA HALL BLUEPRINT
 *    Stores the overall screen design, dimensions, and structural elements.
 *    This is the "big picture" of how the cinema hall is organized.
 *    
 *    Example layoutJson structure:
 *    {
 *      "version": "1.0",
 *      "width": 1000,           // Canvas width in pixels
 *      "height": 800,           // Canvas height in pixels
 *      "background": "#f0f0f0", // Background color
 *      "screenPosition": {      // Where to display "SCREEN" text
 *        "x": 350, "y": 50, "width": 300, "height": 20
 *      },
 *      "aisles": [              // Walkway definitions
 *        {
 *          "id": "main_aisle",
 *          "x": 500, "y": 100, "width": 40, "height": 500,
 *          "label": "Main Aisle", "color": "#d4d4d4"
 *        }
 *      ],
 *      "rows": [                // Row organization (for normal screens)
 *        {
 *          "id": "rowA", "label": "A", "y": 100, "height": 40,
 *          "seatType": "REGULAR", "spacing": 5
 *        }
 *      ],
 *      "sofas": [               // Sofa groups (for VIP screens)
 *        {
 *          "id": "sofa1", "x": 100, "y": 100, "width": 240, "height": 60,
 *          "type": "SOFA_3_SEATER", "mustSellTogether": true
 *        }
 *      ],
 *      "metadata": {            // Screen statistics
 *        "totalSeats": 150, "capacity": 150,
 *        "regularSeats": 100, "premiumSeats": 30, "goldSeats": 20
 *      }
 *    }
 * 
 * 2. SEAT METADATA (ScreenSeat.metaJson) - INDIVIDUAL SEAT POSITIONS
 *    Each physical seat has its own metaJson storing exact position and appearance.
 *    This tells the frontend exactly where to draw each seat on the canvas.
 *    
 *    Example metaJson for a single seat:
 *    {
 *      "x": 100,        // Horizontal position from left (pixels)
 *      "y": 100,        // Vertical position from top (pixels)
 *      "w": 40,         // Seat width (pixels)
 *      "h": 40,         // Seat height (pixels)
 *      "rotation": 0,   // Rotation angle in degrees (0 = straight)
 *      "type": "REGULAR"// Seat type for styling (REGULAR|PREMIUM|GOLD|VIP)
 *    }
 * 
 * VISUAL REPRESENTATION - HOW IT WORKS TOGETHER:
 * 
 *    Screen.layoutJson defines the canvas and structure:
 *    ┌─────────────────────────────────────────────────────┐
 *    │ width: 1000px, height: 800px, background: #f0f0f0  │
 *    │                                                     │
 *    │              🎬 SCREEN 🎬 (x:350, y:50)           │
 *    │                                                     │
 *    │                                                     │
 *    │        AISLE (x:500, y:100, width:40)              │
 *    │                                                     │
 *    │                                                     │
 *    │ ROW A (y:100)    ROW B (y:150)    ROW C (y:200)    │
 *    │                                                     │
 *    └─────────────────────────────────────────────────────┘
 * 
 *    ScreenSeat.metaJson places individual seats:
 *    ┌─────────────────────────────────────────────────────┐
 *    │                                                     │
 *    │              🎬 SCREEN 🎬                          │
 *    │                                                     │
 *    │    [A1]  [A2]  [A3]  [A4]  [A5]    ← Row A         │
 *    │    (100) (150) (200) (250) (300)   ← x coordinates  │
 *    │     ↑     ↑     ↑     ↑     ↑                       │
 *    │    y=100 (same for all in row A)                    │
 *    │                                                     │
 *    │    [B1]  [B2]  [B3]  [B4]  [B5]    ← Row B         │
 *    │    (100) (150) (200) (250) (300)   ← x coordinates  │
 *    │     ↑     ↑     ↑     ↑     ↑                       │
 *    │    y=150 (50px below row A)                         │
 *    │                                                     │
 *    └─────────────────────────────────────────────────────┘
 * 
 * FRONTEND WORKFLOW:
 * 1. Load Screen.layoutJson to create the cinema hall canvas
 * 2. Load all ScreenSeat records with their metaJson
 * 3. For each seat, use metaJson.x and metaJson.y to position it
 * 4. Apply styling based on metaJson.type (REGULAR=PREMIUM=GOLD=VIP)
 * 5. Show real-time availability from ShowSeat states
 * 
 * EXAMPLE FOR 3 SEATS:
 * 
 * Seat A1: {"x": 100, "y": 100, "w": 40, "h": 40, "type": "REGULAR"}
 * Seat A2: {"x": 150, "y": 100, "w": 40, "h": 40, "type": "REGULAR"} 
 * Seat B1: {"x": 100, "y": 150, "w": 40, "h": 40, "type": "PREMIUM"}
 * 
 * Renders as:
 *     ┌─────────────────┐
 *     │     SCREEN      │
 *     └─────────────────┘
 *       100px    150px
 *         ↓        ↓
 *     [ A1 ]    [ A2 ]    ← y=100px (Row A)
 *     [ B1 ]              ← y=150px (Row B)
 *         ↑
 *      100px
 * 
 * BENEFITS OF THIS DESIGN:
 * - Flexible: Supports any seat arrangement (rows, circles, sofas)
 * - Maintainable: Change layouts via database, no code deployment
 * - Scalable: Easy to add new screen types
 * - Real-time: 4 POS systems sync seat states instantly
 * - Historical: ShowSeat snapshots preserve booking history
 * 
 * USAGE IN NIGERIAN CINEMA CONTEXT:
 * - Normal Screens: Traditional row-column layouts
 * - VIP Screens: Luxury sofa groups with must-sell-together logic
 * - Localization: Supports Nigerian audience preferences
 * - Offline POS: No internet dependency for seat rendering
 */