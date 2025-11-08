package com.telecamnig.cinemapos.service;

/**
 * Service interface for managing seat hold expiration and automatic release.
 * 
 * BUSINESS RULES:
 * - Seats are held for maximum 5 minutes (300 seconds)
 * - After 5 minutes, held seats are automatically released
 * - Released seats become AVAILABLE for other customers
 * - Automatic cleanup every 30 seconds to check for expired holds
 * 
 * This prevents seats from being permanently held when:
 * - Customer changes mind
 * - Counter staff forgets to release seats  
 * - POS system crashes/disconnects
 * 
 * @author Your Name
 * @version 1.0
 */
public interface SeatHoldService {

    /**
     * Holds a seat for a specific session with 5-minute expiration.
     * Updates database, broadcasts via WebSocket, and tracks for cleanup.
     *
     * @param sessionId The WebSocket session ID
     * @param showPublicId The show identifier
     * @param seatPublicId The seat to hold
     * @param reservedBy The user who reserved the seat
     */
    void holdSeat(String sessionId, String showPublicId, String seatPublicId, String reservedBy);

    /**
     * Releases a specific seat hold.
     * Updates database, broadcasts via WebSocket, and removes tracking.
     *
     * @param seatPublicId The seat to release
     * @param showPublicId The show identifier for broadcasting
     */
    void releaseSeat(String seatPublicId, String showPublicId);

    /**
     * Releases all seats held by a specific session.
     * Critical: Called when POS disconnects unexpectedly to prevent stuck seats.
     *
     * @param sessionId The session to cleanup
     */
    void releaseSeatsBySession(String sessionId);

    /**
     * Confirms a seat hold (converts HELD to SOLD when booking confirmed).
     * Removes the seat from hold tracking.
     *
     * @param seatPublicId The seat that was sold
     */
    void confirmSeatHold(String seatPublicId);

    /**
     * Gets the remaining hold time for a seat in seconds.
     *
     * @param seatPublicId The seat to check
     * @return Remaining hold time in seconds, or 0 if not held or expired
     */
    long getRemainingHoldTime(String seatPublicId);
    
}