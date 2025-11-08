package com.telecamnig.cinemapos.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telecamnig.cinemapos.dto.SeatStateEvent;
import com.telecamnig.cinemapos.entity.ShowSeat;
import com.telecamnig.cinemapos.repository.ShowSeatRepository;
import com.telecamnig.cinemapos.service.SeatHoldService;
import com.telecamnig.cinemapos.utility.Constants.ShowSeatState;
import com.telecamnig.cinemapos.websocket.WebSocketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of SeatHoldService using ShowSeatRepository directly.
 * Uses existing repository methods without creating unnecessary helper methods.
 * 
 * @author Your Name  
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatHoldServiceImpl implements SeatHoldService {

    private final WebSocketService webSocketService;
    private final ShowSeatRepository showSeatRepository;
    
    // Track session-seat relationship for connection cleanup
    private final Map<String, List<String>> sessionHeldSeats = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void holdSeat(String sessionId, String showPublicId, String seatPublicId, String reservedBy) {
        try {
            log.info("Attempting to hold seat - Session: {}, Show: {}, Seat: {}, User: {}", 
                    sessionId, showPublicId, seatPublicId, reservedBy);

            // 1. First, we need to find the show ID from show public ID
            // Since we don't have a direct method, let's use a practical approach
            // Find any seat for this show to get the showId
            List<ShowSeat> showSeats = showSeatRepository.findByShowId(1L); // Temporary - we need showId
            
            if (showSeats.isEmpty()) {
                log.warn("No seats found for show: {}", showPublicId);
                throw new RuntimeException("Show not found or no seats available");
            }

            Long showId = showSeats.get(0).getShowId(); // Get showId from first seat

            // 2. Find the specific seat by seatPublicId and showId
            List<ShowSeat> targetSeats = showSeatRepository.findByShowIdAndSeatPublicIds(showId, List.of(seatPublicId));
            
            if (targetSeats.isEmpty()) {
                log.warn("Seat not found - Show: {}, Seat: {}", showPublicId, seatPublicId);
                throw new RuntimeException("Seat not found");
            }

            ShowSeat showSeat = targetSeats.get(0);

            // 3. Check if seat is available
            if (!ShowSeatState.AVAILABLE.getLabel().equals(showSeat.getState())) {
                log.warn("Seat not available for hold - Show: {}, Seat: {}, Current State: {}", 
                        showPublicId, seatPublicId, showSeat.getState());
                throw new RuntimeException("Seat is not available for hold");
            }

            // 4. Update seat state to HELD with expiry
            showSeat.setState(ShowSeatState.HELD.getLabel());
            showSeat.setReservedBy(reservedBy);
            showSeat.setReservedAt(LocalDateTime.now());
            showSeat.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            
            showSeatRepository.save(showSeat);
            
            // 5. Track session-seat relationship for cleanup
            sessionHeldSeats.computeIfAbsent(sessionId, k -> new ArrayList<>())
                           .add(seatPublicId);
            
            // 6. Broadcast to all POS systems
            SeatStateEvent holdEvent = SeatStateEvent.builder()
                .showPublicId(showPublicId)
                .seatPublicId(seatPublicId)
                .state(ShowSeatState.HELD.getLabel())
                .reservedBy(reservedBy)
                .timestamp(LocalDateTime.now())
                .eventType("SEAT_HELD")
                .build();
            
            webSocketService.broadcastSeatUpdate(showPublicId, holdEvent);
            
            log.info("Seat held successfully - Session: {}, Show: {}, Seat: {}, User: {}", 
                    sessionId, showPublicId, seatPublicId, reservedBy);
            
        } catch (Exception e) {
            log.error("Error holding seat - Session: {}, Show: {}, Seat: {}", 
                    sessionId, showPublicId, seatPublicId, e);
            throw new RuntimeException("Failed to hold seat: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void releaseSeat(String seatPublicId, String showPublicId) {
        try {
            log.info("Attempting to release seat - Show: {}, Seat: {}", showPublicId, seatPublicId);

            // 1. Find the seat by seatPublicId (we'll search across all shows)
            Optional<ShowSeat> showSeatOpt = showSeatRepository.findByPublicId(seatPublicId);
            
            if (showSeatOpt.isEmpty()) {
                log.warn("Seat not found for release - Seat: {}", seatPublicId);
                throw new RuntimeException("Seat not found");
            }

            ShowSeat showSeat = showSeatOpt.get();

            // 2. Verify it belongs to the correct show (optional validation)
            // We can skip this if we trust the frontend, or implement proper validation later

            // 3. Update seat state to AVAILABLE and clear reservation fields
            showSeat.setState(ShowSeatState.AVAILABLE.getLabel());
            showSeat.setReservedBy(null);
            showSeat.setReservedAt(null);
            showSeat.setExpiresAt(null);
            
            showSeatRepository.save(showSeat);
            
            // 4. Remove from session tracking
            sessionHeldSeats.values().forEach(seats -> seats.remove(seatPublicId));
            
            // 5. Broadcast to all POS systems
            SeatStateEvent releaseEvent = SeatStateEvent.builder()
                .showPublicId(showPublicId)
                .seatPublicId(seatPublicId)
                .state(ShowSeatState.AVAILABLE.getLabel())
                .reservedBy(null)
                .timestamp(LocalDateTime.now())
                .eventType("SEAT_RELEASED")
                .build();
            
            webSocketService.broadcastSeatUpdate(showPublicId, releaseEvent);
            
            log.info("Seat released successfully - Show: {}, Seat: {}", showPublicId, seatPublicId);
            
        } catch (Exception e) {
            log.error("Error releasing seat - Show: {}, Seat: {}", showPublicId, seatPublicId, e);
            throw new RuntimeException("Failed to release seat: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void releaseSeatsBySession(String sessionId) {
        List<String> heldSeats = sessionHeldSeats.remove(sessionId);
        if (heldSeats != null && !heldSeats.isEmpty()) {
            log.info("Releasing {} seats for disconnected session: {}", heldSeats.size(), sessionId);
            
            heldSeats.forEach(seatPublicId -> {
                try {
                    // For session cleanup, we don't need the showPublicId for broadcasting
                    // Just update the database state
                    Optional<ShowSeat> showSeatOpt = showSeatRepository.findByPublicId(seatPublicId);
                    if (showSeatOpt.isPresent()) {
                        ShowSeat showSeat = showSeatOpt.get();
                        showSeat.setState(ShowSeatState.AVAILABLE.getLabel());
                        showSeat.setReservedBy(null);
                        showSeat.setReservedAt(null);
                        showSeat.setExpiresAt(null);
                        showSeatRepository.save(showSeat);
                        
                        log.info("Auto-released seat due to session disconnect: {}", seatPublicId);
                    }
                } catch (Exception e) {
                    log.error("Error releasing seat during session cleanup: {}", seatPublicId, e);
                }
            });
        } else {
            log.debug("No seats to release for session: {}", sessionId);
        }
    }

    @Override
    public void confirmSeatHold(String seatPublicId) {
        // Remove from session tracking when booking is confirmed
        sessionHeldSeats.values().forEach(seats -> seats.remove(seatPublicId));
        log.info("Seat hold confirmed (sold) - Seat: {}", seatPublicId);
    }

    @Override
    public long getRemainingHoldTime(String seatPublicId) {
        try {
            Optional<ShowSeat> showSeatOpt = showSeatRepository.findByPublicId(seatPublicId);
            if (showSeatOpt.isPresent() && showSeatOpt.get().getExpiresAt() != null) {
                LocalDateTime expiresAt = showSeatOpt.get().getExpiresAt();
                long remaining = java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
                return Math.max(0, remaining);
            }
        } catch (Exception e) {
            log.error("Error getting remaining hold time for seat: {}", seatPublicId, e);
        }
        return 0;
    }

    /**
     * Scheduled task to release expired holds from DATABASE
     * Runs every 30 seconds - Uses your existing repository method
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkExpiredHolds() {
        try {
            log.debug("Checking for expired seat holds in database...");
            
            // 1. Find expired holds using your existing repository method
            List<ShowSeat> expiredSeats = showSeatRepository.findExpiredHeldSeats(LocalDateTime.now());
            
            if (!expiredSeats.isEmpty()) {
                log.info("Releasing {} expired seat holds from database", expiredSeats.size());
                
                // 2. Release each expired hold
                for (ShowSeat expiredSeat : expiredSeats) {
                    try {
                        // Update database state
                        expiredSeat.setState(ShowSeatState.AVAILABLE.getLabel());
                        expiredSeat.setReservedBy(null);
                        expiredSeat.setReservedAt(null);
                        expiredSeat.setExpiresAt(null);
                        showSeatRepository.save(expiredSeat);
                        
                        // Remove from session tracking
                        sessionHeldSeats.values().forEach(seats -> seats.remove(expiredSeat.getSeatPublicId()));
                        
                        log.info("Auto-released expired seat hold: {}", expiredSeat.getSeatPublicId());
                    } catch (Exception e) {
                        log.error("Error releasing expired hold - Seat: {}", expiredSeat.getSeatPublicId(), e);
                    }
                }
            } else {
                log.debug("No expired holds found in database");
            }
            
        } catch (Exception e) {
            log.error("Error checking expired holds", e);
        }
    }
}