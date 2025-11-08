package com.telecamnig.cinemapos.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.telecamnig.cinemapos.utility.Constants.BookingStatus;

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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Booking (Ticket) entity — Immutable record of confirmed sales.
 * 
 * CRITICAL DESIGN PRINCIPLES:
 * - IMMUTABLE: Once created, core fields (price, customer info, seat) should NOT change
 * - HISTORICAL: Represents the exact state at booking time, even if shows/seats change later
 * - CANONICAL: Source of truth for revenue reporting, refunds, and audit trails
 * 
 * Nigerian Cinema Context:
 * - Supports multiple payment methods common in Nigeria
 * - Captures customer details for marketing and contact tracing
 * - Tracks printing for physical ticket management
 */
@Entity
@Table(name = "bookings",
       indexes = {
           @Index(name = "idx_bookings_publicid", columnList = "public_id"),
           @Index(name = "idx_bookings_showpublicid", columnList = "show_public_id"),
           @Index(name = "idx_bookings_seatpublicid", columnList = "seat_public_id"),
           @Index(name = "idx_bookings_customerphone", columnList = "customer_phone"),
           @Index(name = "idx_bookings_bookedat", columnList = "booked_at"),
           @Index(name = "idx_bookings_status_bookedat", columnList = "status, booked_at"),
           @Index(name = "idx_bookings_payment_bookedat", columnList = "payment_mode, booked_at"),
           @Index(name = "idx_bookings_bookedby", columnList = "booked_by_user_id")
       },
       uniqueConstraints = {
           // Prevent duplicate bookings for same seat in same show
           @UniqueConstraint(name = "uq_booking_show_seat", columnNames = {"show_public_id", "seat_public_id"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External stable UUID for this booking/ticket.
     * Used in QR codes, ticket numbers, and external API references.
     */
    @NotBlank
    @Size(max = 36)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    // ========== SHOW & SEAT REFERENCES ==========

    /**
     * Reference to the show (using publicId for historical consistency).
     * Even if show is deleted, this booking remains valid.
     */
    @NotBlank
    @Size(max = 36)
    @Column(name = "show_public_id", nullable = false, length = 36)
    private String showPublicId;

    /**
     * Reference to the specific seat (using publicId for historical consistency).
     * Preserved even if physical seat is reconfigured later.
     */
    @NotBlank
    @Size(max = 36)
    @Column(name = "seat_public_id", nullable = false, length = 36)
    private String seatPublicId;

    // ========== SHOW DETAILS (QUICK ACCESS) ==========

    /**
     * Show start time for quick reference without joining shows table.
     * Useful for reporting and ticket printing.
     */
    @NotNull
    @Column(name = "show_start_time", nullable = false)
    private LocalDateTime showStartTime;

    /**
     * Show end time for quick reference and duration calculations.
     */
    @NotNull
    @Column(name = "show_end_time", nullable = false)
    private LocalDateTime showEndTime;

    /**
     * Screen name for quick display on tickets and reports.
     * Avoids joining screens table for basic ticket information.
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "screen_name", nullable = false, length = 100)
    private String screenName;

    // ========== PRICING & PAYMENT (IMMUTABLE) ==========

    /**
     * Final price charged at booking time.
     * IMMUTABLE after creation - preserves historical revenue data.
     */
    @NotNull
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    /**
     * Payment method used for this booking.
     * Common Nigerian methods: CASH, POS, TRANSFER, USSD, etc.
     */
    @NotBlank
    @Size(max = 50)
    @Column(name = "payment_mode", nullable = false, length = 50)
    private String paymentMode;

    /**
     * Optional transaction reference for electronic payments.
     * Useful for reconciliation with bank/POS records.
     */
    @Size(max = 100)
    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    // ========== CUSTOMER DETAILS ==========

    /**
     * Customer full name for ticket printing and identification.
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    /**
     * Customer phone number (essential for Nigerian context).
     * Used for SMS notifications, contact tracing, and marketing.
     */
    @NotBlank
    @Size(max = 20)
    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    /**
     * Optional email for digital tickets and receipts.
     */
    @Email
    @Size(max = 100)
    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    // ========== BOOKING METADATA ==========

    /**
     * Staff member who processed this booking.
     * Using User.id (Long) for fast database joins and internal references.
     */
    @NotNull
    @Column(name = "booked_by_user_id", nullable = false)
    private Long bookedByUserId;

    /**
     * Timestamp when booking was confirmed (immutable).
     * Different from createdAt (when record was inserted in DB).
     */
    @NotNull
    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    /**
     * Booking lifecycle status.
     * ISSUED -> Active ticket
     * CANCELLED -> Cancelled before show
     * REFUNDED -> Cancelled with refund after issuance
     */
    @NotBlank
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Number of times this ticket has been printed.
     * Prevents unlimited reprinting while allowing replacements.
     */
    @NotNull
    @Column(name = "print_count", nullable = false)
    private Integer printCount;

    /**
     * Optional notes for staff (refund reasons, special handling, etc.).
     */
    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

    // ========== AUDIT FIELDS ==========

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    // ========== LIFE CYCLE HOOKS ==========

    @PrePersist
    protected void onCreate() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = BookingStatus.ISSUED.getLabel();
        }
        if (this.printCount == null) {
            this.printCount = 0;
        }
        if (this.bookedAt == null) {
            this.bookedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Add validation logic if needed
        // Example: Ensure price doesn't change after creation
    }

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Check if booking is active (not cancelled or refunded).
     */
    public boolean isActive() {
        return BookingStatus.ISSUED.getLabel().equals(this.status);
    }

    /**
     * Check if booking can be cancelled (usually before show start).
     */
    public boolean isCancellable() {
        return isActive(); // Add show time logic in service layer
    }

    /**
     * Increment print count when ticket is printed.
     */
    public void incrementPrintCount() {
        if (this.printCount == null) {
            this.printCount = 0;
        }
        this.printCount++;
    }

    /**
     * Cancel this booking with optional reason.
     */
    public void cancel(String reason) {
        this.status = BookingStatus.CANCELLED.getLabel();
        if (reason != null && !reason.isBlank()) {
            this.notes = (this.notes != null ? this.notes + "\n" : "") + 
                        "Cancelled: " + reason;
        }
    }

    /**
     * Process refund for this booking.
     */
    public void refund(String reason) {
        this.status = BookingStatus.REFUNDED.getLabel();
        if (reason != null && !reason.isBlank()) {
            this.notes = (this.notes != null ? this.notes + "\n" : "") + 
                        "Refunded: " + reason;
        }
    }

    /* ------------------ Developer Notes ------------------
     *
     * IMMUTABILITY RULES:
     * - Once created, these fields should NEVER change:
     *   publicId, showPublicId, seatPublicId, price, customerName, 
     *   customerPhone, customerEmail, bookedAt, paymentMode,
     *   showStartTime, showEndTime, screenName, bookedByUserId
     *
     * - For corrections, create a new booking and cancel the old one
     *
     * PERFORMANCE OPTIMIZATIONS:
     * - showStartTime/showEndTime: Avoid joining shows table for tickets
     * - screenName: Avoid joining screens table for basic info  
     * - bookedByUserId: Fast Long reference instead of String UUID joins
     *
     * NIGERIAN CONTEXT:
     * - Phone numbers are essential for customer communication
     * - Multiple payment methods support (cash still very common)
     *
     * REPORTING:
     * - This table is the source of truth for revenue reports
     * - Use booked_at for daily/weekly/monthly sales reports
     * - Group by payment_mode for payment method analysis
     *
     * SECURITY:
     * - Never expose internal IDs (id) in external APIs
     * - Use publicId for all external references
     * - Consider encrypting customer PII in future
     *
     * ------------------------------------------------------ */
}