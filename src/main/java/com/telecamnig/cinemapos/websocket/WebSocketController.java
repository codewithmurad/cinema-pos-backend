package com.telecamnig.cinemapos.websocket;

import java.time.LocalDateTime;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.telecamnig.cinemapos.dto.SeatStateEvent;
import com.telecamnig.cinemapos.service.SeatHoldService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket Controller for handling real-time messaging between POS systems.
 *
 * MESSAGE FLOW:
 * 1. POS client connects to /ws endpoint
 * 2. Client subscribes to /topic/shows/{showId}/seats for seat updates
 * 3. When seat state changes, server broadcasts via WebSocketService
 * 4. All POS systems update their UI in real-time
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SeatHoldService seatHoldService;
    private final WebSocketService webSocketService;   // wrapper over SimpMessagingTemplate

    /**
     * Handles seat hold requests from POS clients.
     *
     * Client → /app/seats/hold
     * Server broadcasts to → /topic/shows/{showPublicId}/seats
     *
     * Expected payload (SeatStateEvent):
     * - showPublicId
     * - seatPublicId
     * - reservedBy (counter username)
     * - state = "HELD"
     */
    @MessageMapping("/seats/hold")
    public void handleSeatHold(SeatStateEvent seatEvent, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String user = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "unknown";

        log.info("Seat hold request received - Session: {}, User: {}, Show: {}, Seat: {}",
                sessionId, user, seatEvent.getShowPublicId(), seatEvent.getSeatPublicId());

        // 1️⃣ Basic validation of the incoming event
        if (!isValidSeatHoldRequest(seatEvent)) {
            log.warn("Invalid seat hold request from session: {}", sessionId);
            sendErrorToTopic(seatEvent.getShowPublicId(), seatEvent, "Invalid seat hold request");
            return;
        }

        try {
            // 2️⃣ Delegate business logic to SeatHoldService
            //    This will:
            //    - validate that seat is AVAILABLE
            //    - mark it HELD in DB, set expiry
            //    - track seat against WebSocket session
            //    - broadcast HELD state via WebSocketService.broadcastSeatUpdate(...)
            seatHoldService.holdSeat(
                    sessionId,
                    seatEvent.getShowPublicId(),
                    seatEvent.getSeatPublicId(),
                    seatEvent.getReservedBy()
            );

            log.info("Seat hold processed successfully - Show: {}, Seat: {}, User: {}",
                    seatEvent.getShowPublicId(), seatEvent.getSeatPublicId(), seatEvent.getReservedBy());

            // ⚠️ NO DIRECT SEND HERE
            // SeatHoldServiceImpl already broadcasts HELD state using WebSocketService

        } catch (Exception e) {
            log.error("Error processing seat hold - Show: {}, Seat: {}",
                    seatEvent.getShowPublicId(), seatEvent.getSeatPublicId(), e);

            // 3️⃣ If any error, send an ERROR event to the same show topic
            sendErrorToTopic(seatEvent.getShowPublicId(), seatEvent,
                    "Failed to hold seat: " + e.getMessage());
        }
    }

    /**
     * Handles seat release requests from POS clients.
     *
     * Client → /app/seats/release
     * Server broadcasts to → /topic/shows/{showPublicId}/seats
     *
     * Expected payload (SeatStateEvent):
     * - showPublicId
     * - seatPublicId
     * - reservedBy (counter username trying to release)
     * - state = "AVAILABLE" (from frontend perspective)
     *
     * Business rule:
     * - Only the same user who HELD the seat can RELEASE it.
     *   That check is implemented in SeatHoldServiceImpl.releaseSeat(...)
     */
    @MessageMapping("/seats/release")
    public void handleSeatRelease(SeatStateEvent seatEvent, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        log.info("Seat release request received - Session: {}, Show: {}, Seat: {}, RequestedBy: {}",
                sessionId,
                seatEvent.getShowPublicId(),
                seatEvent.getSeatPublicId(),
                seatEvent.getReservedBy()
        );

        try {
            // 1️⃣ Call the new three-argument method:
            //    releaseSeat(seatPublicId, showPublicId, reservedBy)
            //
            // Inside SeatHoldServiceImpl:
            //    if (showSeat.getReservedBy() != null
            //        && reservedBy != null
            //        && !reservedBy.equals(showSeat.getReservedBy())) {
            //        throw new RuntimeException("Cannot release seat held by another user");
            //    }
            //
            // So this enforces: only owner of the hold can release.
            seatHoldService.releaseSeat(
                    seatEvent.getSeatPublicId(),
                    seatEvent.getShowPublicId(),
                    seatEvent.getReservedBy()
            );

            log.info("Seat release processed successfully - Show: {}, Seat: {}",
                    seatEvent.getShowPublicId(), seatEvent.getSeatPublicId());

            // ⚠️ NO DIRECT BROADCAST HERE
            // SeatHoldServiceImpl.releaseSeat(...) already:
            //   - updates DB state to AVAILABLE
            //   - clears reservedBy / timestamps
            //   - broadcasts SEAT_RELEASED via WebSocketService.broadcastSeatUpdate(...)

        } catch (Exception e) {
            log.error("Error processing seat release - Show: {}, Seat: {}, Error: {}",
                    seatEvent.getShowPublicId(),
                    seatEvent.getSeatPublicId(),
                    e.getMessage()
            );

            // 2️⃣ Broadcast an ERROR event so UI can show a toast / revert
            sendErrorToTopic(seatEvent.getShowPublicId(), seatEvent,
                    "Failed to release seat: " + e.getMessage());
        }
    }

    /**
     * Sends an ERROR SeatStateEvent to the show's WebSocket topic.
     *
     * This is used when:
     * - validation fails
     * - seat cannot be held/released
     * - internal exception occurs
     */
    private void sendErrorToTopic(String showPublicId, SeatStateEvent originalEvent, String errorMessage) {
        SeatStateEvent errorEvent = SeatStateEvent.builder()
                .showPublicId(showPublicId)
                .seatPublicId(originalEvent.getSeatPublicId())
                .state("ERROR")
                .reservedBy(originalEvent.getReservedBy())
                .timestamp(LocalDateTime.now())
                .eventType("ERROR")
                .build();

        log.warn("Sending error event for show: {}, seat: {}, message: {}",
                showPublicId, originalEvent.getSeatPublicId(), errorMessage);

        webSocketService.broadcastSeatUpdate(showPublicId, errorEvent);
    }

    /**
     * Heartbeat / ping from POS clients.
     * 
     * Client → /app/heartbeat
     * Used for monitoring active connections if needed.
     */
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(String message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String user = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "unknown";

        log.debug("Heartbeat received - Session: {}, User: {}, Message: {}",
                sessionId, user, message);
    }

    /**
     * Validates seat hold requests from clients before processing.
     *
     * Checks:
     * - showPublicId not blank
     * - seatPublicId not blank
     * - reservedBy not blank
     * - state == "HELD"
     */
    private boolean isValidSeatHoldRequest(SeatStateEvent seatEvent) {
        if (seatEvent.getShowPublicId() == null || seatEvent.getShowPublicId().trim().isEmpty()) {
            return false;
        }
        if (seatEvent.getSeatPublicId() == null || seatEvent.getSeatPublicId().trim().isEmpty()) {
            return false;
        }
        if (seatEvent.getReservedBy() == null || seatEvent.getReservedBy().trim().isEmpty()) {
            return false;
        }
        if (!"HELD".equals(seatEvent.getState())) {
            return false;
        }
        return true;
    }
}
