package com.telecamnig.cinemapos.websocket;

import java.time.LocalDateTime;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.telecamnig.cinemapos.dto.SeatStateEvent;
import com.telecamnig.cinemapos.service.SeatHoldService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket Controller for handling real-time messaging between POS systems.
 * 
 * This controller processes WebSocket messages from clients and broadcasts
 * updates to all connected POS systems for real-time synchronization.
 * 
 * MESSAGE FLOW:
 * 1. POS client connects to /ws endpoint
 * 2. Client subscribes to /topic/shows/{showId}/seats for seat updates
 * 3. When seat state changes, server broadcasts to all subscribers
 * 4. All POS systems update their UI in real-time
 * 
 * @author Your Name
 * @version 1.0
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SeatHoldService seatHoldService;
    
    private final SimpMessagingTemplate messagingTemplate; // ✅ Add this

    /**
     * Handles seat hold requests from POS clients.
     * 
     * When a counter staff selects a seat for booking, the POS client sends
     * a hold request to temporarily reserve the seat while the customer completes payment.
     * 
     * MESSAGE ROUTING:
     * - Client sends to: /app/seats/hold
     * - Server broadcasts to: /topic/shows/{showId}/seats
     * 
     * @param seatEvent The seat hold request from client
     * @param headerAccessor WebSocket message headers
     * @return SeatStateEvent broadcast to all subscribers
     */
    @MessageMapping("/seats/hold")
    public void handleSeatHold(SeatStateEvent seatEvent, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String user = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "unknown";
        
        log.info("Seat hold request received - Session: {}, User: {}, Show: {}, Seat: {}", 
                sessionId, user, seatEvent.getShowPublicId(), seatEvent.getSeatPublicId());
        
        // Validate the seat hold request
        if (!isValidSeatHoldRequest(seatEvent)) {
            log.warn("Invalid seat hold request from session: {}", sessionId);
            // Send error directly to the topic
            sendErrorToTopic(seatEvent.getShowPublicId(), seatEvent, "Invalid seat hold request");
            return;
        }
        
        try {
            // Process the seat hold through our service
            seatHoldService.holdSeat(sessionId, seatEvent.getShowPublicId(), 
                                   seatEvent.getSeatPublicId(), seatEvent.getReservedBy());
            
            log.info("Seat hold processed successfully - Show: {}, Seat: {}, User: {}",
                    seatEvent.getShowPublicId(), seatEvent.getSeatPublicId(), seatEvent.getReservedBy());
            
            // ✅ Send success response to the specific show topic
            messagingTemplate.convertAndSend("/topic/shows/" + seatEvent.getShowPublicId() + "/seats", seatEvent);
            
        } catch (Exception e) {
            log.error("Error processing seat hold - Show: {}, Seat: {}", 
                    seatEvent.getShowPublicId(), seatEvent.getSeatPublicId(), e);
            // ✅ Send error to the specific show topic
            sendErrorToTopic(seatEvent.getShowPublicId(), seatEvent, "Failed to hold seat: " + e.getMessage());
        }
    }

    /**
     * Handles seat release requests from POS clients.
     * 
     * When a seat hold expires or is manually released, this method broadcasts
     * the release to all connected POS systems.
     * 
     * @param seatEvent The seat release request
     * @param headerAccessor WebSocket message headers
     * @return SeatStateEvent broadcast to all subscribers
     */
    @MessageMapping("/seats/release")
    public void handleSeatRelease(SeatStateEvent seatEvent, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        
        log.info("Seat release request received - Session: {}, Show: {}, Seat: {}", 
                sessionId, seatEvent.getShowPublicId(), seatEvent.getSeatPublicId());
        
        try {
            // Process the seat release through our service
            seatHoldService.releaseSeat(seatEvent.getSeatPublicId(), seatEvent.getShowPublicId());
            
            log.info("Seat release processed successfully - Show: {}, Seat: {}",
                    seatEvent.getShowPublicId(), seatEvent.getSeatPublicId());
            
            // ✅ Send release confirmation to the specific show topic
            messagingTemplate.convertAndSend("/topic/shows/" + seatEvent.getShowPublicId() + "/seats", seatEvent);
            
        } catch (Exception e) {
            log.error("Error processing seat release - Show: {}, Seat: {}", 
                    seatEvent.getShowPublicId(), seatEvent.getSeatPublicId(), e);
            sendErrorToTopic(seatEvent.getShowPublicId(), seatEvent, "Failed to release seat: " + e.getMessage());
        }
    }

    /**
     * Sends error message to specific show topic
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
        
        messagingTemplate.convertAndSend("/topic/shows/" + showPublicId + "/seats", errorEvent);
    }

    /**
     * Handles heartbeat/ping messages from clients.
     * 
     * POS clients send periodic heartbeat messages to maintain connection
     * and confirm they're still active.
     * 
     * @param message The heartbeat message
     * @param headerAccessor WebSocket message headers
     */
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(String message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String user = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "unknown";
        
        log.debug("Heartbeat received - Session: {}, User: {}, Message: {}", 
                sessionId, user, message);
        
        // Update last activity timestamp for this session
        // This can be used for connection monitoring and cleanup
    }

    /**
     * Validates seat hold requests from clients.
     * 
     * @param seatEvent The seat event to validate
     * @return true if valid, false otherwise
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

    /**
     * Creates an error event for invalid requests.
     * 
     * @param originalEvent The original seat event
     * @param errorMessage The error message
     * @return Error seat event
     */
    private SeatStateEvent createErrorEvent(SeatStateEvent originalEvent, String errorMessage) {
        return SeatStateEvent.builder()
                .showPublicId(originalEvent.getShowPublicId())
                .seatPublicId(originalEvent.getSeatPublicId())
                .state("ERROR")
                .reservedBy(originalEvent.getReservedBy())
                .timestamp(java.time.LocalDateTime.now())
                .eventType("ERROR")
                .build();
    }
}