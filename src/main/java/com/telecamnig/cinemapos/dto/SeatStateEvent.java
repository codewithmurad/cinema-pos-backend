package com.telecamnig.cinemapos.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket event for real-time seat state updates.
 * 
 * This event is broadcast to all connected POS clients when a seat's state changes
 * (e.g., from AVAILABLE to HELD, or from HELD to SOLD).
 * 
 * USAGE:
 * - When a user selects a seat, it becomes HELD and this event is broadcast
 * - When a booking is confirmed, seats become SOLD and this event is broadcast
 * - When a seat hold expires, it becomes AVAILABLE and this event is broadcast
 * 
 * All POS systems listen to these events to keep their UI in sync.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatStateEvent {
    
    /**
     * Unique identifier of the show
     */
    private String showPublicId;
    
    /**
     * Unique identifier of the seat (ScreenSeat.publicId)
     */
    private String seatPublicId;
    
    /**
     * Current state of the seat (AVAILABLE, HELD, SOLD)
     */
    private String state;
    
    /**
     * User who reserved the seat (null if AVAILABLE)
     */
    private String reservedBy;
    
    /**
     * Timestamp when the state change occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Type of event for frontend processing (SEAT_HELD, SEAT_RELEASED, SEAT_SOLD)
     */
    private String eventType;
    
    /**
     * Optional: Booking reference if seat was sold
     */
    private String bookingPublicId;

}