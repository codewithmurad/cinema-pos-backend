package com.telecamnig.cinemapos.service;

/**
 * =====================================================================================
 *  SeatHoldService — Interface for REAL-TIME seat hold management
 * =====================================================================================
 *
 *  PURPOSE:
 *  --------
 *  Defines all operations required for temporary seat reservations
 *  BEFORE an actual booking is confirmed.
 *
 *  USED BY:
 *  --------
 *  - WebSocketController (hold + release events from POS counters)
 *  - BookingService      (confirming seats after payment)
 *  - Scheduled Jobs      (auto-expiring stale seat holds)
 *
 *  WHY THIS INTERFACE?
 *  -------------------
 *  To keep the logic decoupled and future-proof. Multiple implementations
 *  can exist (Redis, Memory, Database, Hybrid) without touching controllers.
 *
 *  CORE RESPONSIBILITIES:
 *  ----------------------
 *  ✔ Hold seat (5 minutes)  
 *  ✔ Release seat manually  
 *  ✔ Release seat on session disconnect  
 *  ✔ Confirm seat after booking  
 *  ✔ Provide remaining time for countdown timer UI  
 *
 * =====================================================================================
 */
public interface SeatHoldService {

    /**
     * TEMPORARILY holds a seat for a counter user for 5 minutes.
     *
     * @param sessionId     WebSocket session ID of the POS client
     * @param showPublicId  Public ID of the show
     * @param seatPublicId  Public ID of the seat being held
     * @param reservedBy    User who is holding the seat (cashier username)
     */
    void holdSeat(String sessionId, String showPublicId, String seatPublicId, String reservedBy);

    /**
     * RELEASES a held seat.
     *
     * SECURITY RULE:
     * Only the same counter user who held the seat can release it.
     *
     * @param seatPublicId  Public ID of the seat being released
     * @param showPublicId  Public ID of the show
     * @param reservedBy    User attempting to release the seat
     */
    void releaseSeat(String seatPublicId, String showPublicId, String reservedBy);

    /**
     * Releases ALL seats when a POS websocket session disconnects.
     * Prevents stuck seats if browser/tab is closed abruptly.
     *
     * @param sessionId WebSocket session ID
     */
    void releaseSeatsBySession(String sessionId);

    /**
     * CONFIRMS a held seat (called after booking success).
     * Removes the seat from session tracking so it never auto-releases again.
     *
     * @param seatPublicId Public ID of the sold seat
     */
    void confirmSeatHold(String seatPublicId);

    /**
     * Returns remaining seconds before seat hold expires – used for UI timer.
     *
     * @param seatPublicId Public ID of the seat
     * @return seconds remaining (0 if expired)
     */
    long getRemainingHoldTime(String seatPublicId);
}
