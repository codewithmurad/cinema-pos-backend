package com.telecamnig.cinemapos.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.telecamnig.cinemapos.dto.BatchSeatStateEvent;
import com.telecamnig.cinemapos.dto.BookingConfirmationEvent;
import com.telecamnig.cinemapos.dto.SeatStateEvent;
import com.telecamnig.cinemapos.dto.SystemEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for broadcasting WebSocket messages to connected POS clients.
 * 
 * This service is used by other parts of the application (BookingService, ShowService)
 * to send real-time updates to all connected POS systems without being tightly coupled
 * to WebSocket implementation details.
 * 
 * USAGE:
 * - When a seat state changes, call broadcastSeatUpdate()
 * - When a booking is confirmed, call broadcastBookingConfirmation()
 * - For system notifications, call broadcastSystemEvent()
 * 
 * @author Your Name
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts a single seat state update to all POS systems.
     * 
     * This method is called when:
     * - A seat is held for booking
     * - A seat is released (hold expired or cancelled)
     * - A seat is sold (booking confirmed)
     * 
     * @param showPublicId The show identifier for topic routing
     * @param seatEvent The seat state change event
     */
    public void broadcastSeatUpdate(String showPublicId, SeatStateEvent seatEvent) {
        try {
            String destination = "/topic/shows/" + showPublicId + "/seats";
            messagingTemplate.convertAndSend(destination, seatEvent);
            
            log.debug("Seat update broadcast - Destination: {}, Seat: {}, State: {}", 
                    destination, seatEvent.getSeatPublicId(), seatEvent.getState());
        } catch (Exception e) {
            log.error("Failed to broadcast seat update for show: {}, seat: {}", 
                    showPublicId, seatEvent.getSeatPublicId(), e);
        }
    }

    /**
     * Broadcasts multiple seat state updates in a single message.
     * 
     * More efficient than individual broadcasts when multiple seats
     * change state simultaneously (e.g., sofa group booking, bulk release).
     * 
     * @param showPublicId The show identifier for topic routing
     * @param batchEvent The batch seat update event
     */
    public void broadcastBatchSeatUpdates(String showPublicId, BatchSeatStateEvent batchEvent) {
        try {
            String destination = "/topic/shows/" + showPublicId + "/seats";
            messagingTemplate.convertAndSend(destination, batchEvent);
            
            log.debug("Batch seat updates broadcast - Destination: {}, Seat count: {}", 
                    destination, batchEvent.getSeatUpdates().size());
        } catch (Exception e) {
            log.error("Failed to broadcast batch seat updates for show: {}", showPublicId, e);
        }
    }

    /**
     * Broadcasts booking confirmation to all POS systems.
     * 
     * Used to notify all systems when a booking is successfully completed.
     * This helps maintain consistent booking history across all POS terminals.
     * 
     * @param bookingEvent The booking confirmation event
     */
    public void broadcastBookingConfirmation(BookingConfirmationEvent bookingEvent) {
        try {
            String destination = "/topic/bookings/confirmations";
            messagingTemplate.convertAndSend(destination, bookingEvent);
            
            log.info("Booking confirmation broadcast - Booking: {}, Show: {}, Seats: {}", 
                    bookingEvent.getBookingPublicId(), bookingEvent.getShowPublicId(), 
                    bookingEvent.getSeatLabels());
        } catch (Exception e) {
            log.error("Failed to broadcast booking confirmation: {}", bookingEvent.getBookingPublicId(), e);
        }
    }

    /**
     * Sends a notification to a specific user.
     * 
     * Used for personal notifications like:
     * - Booking confirmation for a specific counter staff
     * - Error messages related to a specific operation
     * - Personal alerts and notifications
     * 
     * @param username The target user
     * @param systemEvent The system event to send
     */
    public void sendUserNotification(String username, SystemEvent systemEvent) {
        try {
            String destination = "/user/" + username + "/notifications";
            messagingTemplate.convertAndSend(destination, systemEvent);
            
            log.debug("User notification sent - User: {}, Type: {}", 
                    username, systemEvent.getType());
        } catch (Exception e) {
            log.error("Failed to send user notification to: {}", username, e);
        }
    }

    /**
     * Broadcasts system-wide events to all connected clients.
     * 
     * Used for:
     * - System maintenance notifications
     * - Global alerts (system going down, etc.)
     * - Broadcast messages to all POS systems
     * 
     * @param systemEvent The system event to broadcast
     */
    public void broadcastSystemEvent(SystemEvent systemEvent) {
        try {
            String destination = "/topic/system/events";
            messagingTemplate.convertAndSend(destination, systemEvent);
            
            log.info("System event broadcast - Type: {}, Message: {}", 
                    systemEvent.getType(), systemEvent.getMessage());
        } catch (Exception e) {
            log.error("Failed to broadcast system event", e);
        }
    }

    /**
     * Initializes seat map for a newly connected client.
     * 
     * When a POS client first connects and subscribes to a show's seat updates,
     * this method can send the current state of all seats for that show.
     * 
     * @param showPublicId The show identifier
     * @param username The target user
     * @param initialSeats The current state of all seats
     */
    public void sendInitialSeatMap(String showPublicId, String username, BatchSeatStateEvent initialSeats) {
        try {
            String destination = "/user/" + username + "/shows/" + showPublicId + "/initial-seats";
            messagingTemplate.convertAndSend(destination, initialSeats);
            
            log.debug("Initial seat map sent - User: {}, Show: {}, Seat count: {}", 
                    username, showPublicId, initialSeats.getSeatUpdates().size());
        } catch (Exception e) {
            log.error("Failed to send initial seat map to user: {} for show: {}", username, showPublicId, e);
        }
    }
    
    /**
     * Broadcasts messages to a specific show topic.
     * Used for show-specific updates like status changes.
     * 
     * @param showPublicId The show identifier
     * @param message The message to broadcast
     */
    public void broadcastToShow(String showPublicId, String message) {
        try {
            String destination = "/topic/shows/" + showPublicId + "/updates";
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("Show-specific broadcast - Destination: {}, Message: {}", destination, message);
        } catch (Exception e) {
            log.error("Failed to broadcast to show: {}", showPublicId, e);
        }
    }

}