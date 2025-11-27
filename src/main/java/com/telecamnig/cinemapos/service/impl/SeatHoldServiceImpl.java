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
import com.telecamnig.cinemapos.entity.Show;
import com.telecamnig.cinemapos.entity.ShowSeat;
import com.telecamnig.cinemapos.repository.ShowRepository;
import com.telecamnig.cinemapos.repository.ShowSeatRepository;
import com.telecamnig.cinemapos.service.SeatHoldService;
import com.telecamnig.cinemapos.utility.Constants.ShowSeatState;
import com.telecamnig.cinemapos.websocket.WebSocketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ================================================================================
 *  SeatHoldServiceImpl — PRODUCTION VERSION
 * ================================================================================
 *
 *  PURPOSE:
 *  --------
 *  This service handles REAL-TIME seat holding logic for all POS counters.
 *
 *  FEATURES:
 *  ---------
 *  ✔ Holds seats for 5 minutes  
 *  ✔ Releases seats manually OR when user switches seat  
 *  ✔ Auto-release expired seats (cron job)  
 *  ✔ Tracks session ↔ held seats for auto cleanup  
 *  ✔ Broadcasts ALL seat events over WebSocket to every POS  
 *
 *  WHY THIS SERVICE EXISTS:
 *  ------------------------
 *  BookingService will confirm seats.
 *  THIS service manages temporary seat holds BEFORE booking.
 *
 *  FLOW:
 *  -----
 *  1. POS selects a seat → WebSocket → /app/seats/hold → this service  
 *  2. Seat is marked HELD in DB  
 *  3. Event is broadcast → all counters update live  
 *  4. If user deselects → releaseSeat()  
 *  5. If booking completes → confirmSeatHold()  
 *  6. If 5 mins pass → auto release (scheduled cleanup)  
 *
 * ================================================================================
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatHoldServiceImpl implements SeatHoldService {

    private final WebSocketService webSocketService;   // Used for broadcasting seat updates
    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;

    /**
     * Stores all seats held by each WebSocket session.
     * Key = sessionId
     * Value = List of seatPublicIds held by this session
     */
    private final Map<String, List<String>> sessionHeldSeats = new ConcurrentHashMap<>();


    // ======================================================================================
    // 1. HOLD SEAT
    // ======================================================================================
    /**
     * Hold a seat temporarily for a POS counter.
     *
     * @param sessionId   WebSocket session ID
     * @param showPublicId The show identifier
     * @param seatPublicId The seat identifier
     * @param reservedBy   User who selected the seat (counter staff name)
     */
    @Override
    @Transactional
    public void holdSeat(String sessionId, String showPublicId, String seatPublicId, String reservedBy) {

        log.info("HoldSeat | Session: {}, Show: {}, Seat: {}, User: {}",
                sessionId, showPublicId, seatPublicId, reservedBy);

        // 1. Get show
        Show show = showRepository.findByPublicId(showPublicId)
                .orElseThrow(() -> new RuntimeException("Show not found"));

        Long showId = show.getId();

        // 2. Fetch seat for that show
        List<ShowSeat> seatList =
                showSeatRepository.findByShowIdAndPublicIds(showId, List.of(seatPublicId));

        if (seatList.isEmpty()) {
            throw new RuntimeException("Seat not found");
        }

        ShowSeat seat = seatList.get(0);

        // 3. Check if AVAILABLE (cannot hold HELD/SOLD seats)
        if (!ShowSeatState.AVAILABLE.getLabel().equals(seat.getState())) {
            throw new RuntimeException("Seat not available for hold");
        }

        // 4. Set seat to HELD
        LocalDateTime now = LocalDateTime.now();

        seat.setState(ShowSeatState.HELD.getLabel());
        seat.setReservedBy(reservedBy);
        seat.setReservedAt(now);
        seat.setExpiresAt(now.plusMinutes(5));   // HOLD TIME = 5 MINUTES

        showSeatRepository.save(seat);

        // 5. Track for cleanup
        sessionHeldSeats.computeIfAbsent(sessionId, x -> new ArrayList<>())
                .add(seatPublicId);

        // 6. Broadcast seat held event
        SeatStateEvent event = SeatStateEvent.builder()
                .showPublicId(showPublicId)
                .seatPublicId(seatPublicId)
                .state(ShowSeatState.HELD.getLabel())
                .reservedBy(reservedBy)
                .timestamp(now)
                .eventType("SEAT_HELD")
                .build();

        webSocketService.broadcastSeatUpdate(showPublicId, event);

        log.info("Seat held successfully — {}", seatPublicId);
    }


    // ======================================================================================
    // 2. RELEASE SEAT
    // ======================================================================================
    /**
     * Releases a seat manually or from UI.
     *
     * VALIDATION:
     * Only the same counter user holding the seat can release it.
     */
    @Override
    @Transactional
    public void releaseSeat(String seatPublicId, String showPublicId, String reservedBy) {

    	log.info("ReleaseSeat | Show: {}, Seat: {}, RequestedBy: {}", showPublicId, seatPublicId, reservedBy);

        // 1. Fetch the seat
        Optional<ShowSeat> seatOpt = showSeatRepository.findByPublicId(seatPublicId);

        if (seatOpt.isEmpty()) {
            throw new RuntimeException("Seat not found");
        }

        ShowSeat seat = seatOpt.get();

        // 2. 🔒 SECURITY: Only the same user who held the seat can release it
        // Check if reservedBy (email) matches the one who originally held the seat
        if (seat.getReservedBy() == null || !seat.getReservedBy().equals(reservedBy)) {
            log.warn("Unauthorized seat release attempt - Seat held by: {}, Requested by: {}",
                    seat.getReservedBy(), reservedBy);
            throw new RuntimeException("Cannot release seat held by another user");
        }

        // 3. Reset seat to AVAILABLE
        seat.setState(ShowSeatState.AVAILABLE.getLabel());
        seat.setReservedBy(null);
        seat.setReservedAt(null);
        seat.setExpiresAt(null);

        showSeatRepository.save(seat);

        // 4. Remove from session tracking
        sessionHeldSeats.values().forEach(list -> list.remove(seatPublicId));

        // 5. Broadcast release event
        SeatStateEvent event = SeatStateEvent.builder()
                .showPublicId(showPublicId)
                .seatPublicId(seatPublicId)
                .state(ShowSeatState.AVAILABLE.getLabel())
                .timestamp(LocalDateTime.now())
                .eventType("SEAT_RELEASED")
                .build();

        webSocketService.broadcastSeatUpdate(showPublicId, event);

        log.info("Seat released successfully — {}", seatPublicId);
    }


    // ======================================================================================
    // 3. RELEASE ALL SEATS WHEN A SESSION DISCONNECTS
    // ======================================================================================
    /**
     * Auto-release seats when the POS browser/tab is closed.
     */
    @Override
    public void releaseSeatsBySession(String sessionId) {

        List<String> seats = sessionHeldSeats.remove(sessionId);

        if (seats == null || seats.isEmpty()) return;

        log.info("Releasing {} seats held by disconnected session {}", seats.size(), sessionId);

        for (String seatPublicId : seats) {

            Optional<ShowSeat> seatOpt = showSeatRepository.findByPublicId(seatPublicId);

            if (seatOpt.isEmpty()) continue;

            ShowSeat seat = seatOpt.get();

            seat.setState(ShowSeatState.AVAILABLE.getLabel());
            seat.setReservedBy(null);
            seat.setReservedAt(null);
            seat.setExpiresAt(null);

            showSeatRepository.save(seat);

            String showPublicId = resolveShowPublicId(seat.getShowId());

            SeatStateEvent event = SeatStateEvent.builder()
                    .showPublicId(showPublicId)
                    .seatPublicId(seatPublicId)
                    .state(ShowSeatState.AVAILABLE.getLabel())
                    .eventType("SEAT_RELEASED")
                    .timestamp(LocalDateTime.now())
                    .build();

            webSocketService.broadcastSeatUpdate(showPublicId, event);
        }
    }


    // ======================================================================================
    // 4. CONFIRM SEAT HOLD (after booking)
    // ======================================================================================
    @Override
    public void confirmSeatHold(String seatPublicId) {
        sessionHeldSeats.values().forEach(list -> list.remove(seatPublicId));
        log.info("Seat confirmed (sold), removing hold tracking — {}", seatPublicId);
    }


    // ======================================================================================
    // 5. GET REMAINING HOLD TIME
    // ======================================================================================
    @Override
    public long getRemainingHoldTime(String seatPublicId) {

        Optional<ShowSeat> seat = showSeatRepository.findByPublicId(seatPublicId);

        if (seat.isPresent() && seat.get().getExpiresAt() != null) {

            long secs = java.time.Duration.between(LocalDateTime.now(), seat.get().getExpiresAt())
                    .getSeconds();

            return Math.max(0, secs);
        }

        return 0;
    }


    // ======================================================================================
    // 6. AUTO RELEASE EXPIRED SEATS — RUNS EVERY 30 SECONDS
    // ======================================================================================
    @Scheduled(fixedRate = 120000)
    @Transactional
    public void checkExpiredHolds() {

        log.debug("Checking for expired seat holds...");

        List<ShowSeat> expiredSeats = showSeatRepository.findExpiredHeldSeats(LocalDateTime.now());

        if (expiredSeats.isEmpty()) return;

        log.info("Found {} expired seats. Releasing...", expiredSeats.size());

        for (ShowSeat seat : expiredSeats) {

            seat.setState(ShowSeatState.AVAILABLE.getLabel());
            seat.setReservedBy(null);
            seat.setReservedAt(null);
            seat.setExpiresAt(null);

            showSeatRepository.save(seat);

            sessionHeldSeats.values().forEach(list -> list.remove(seat.getSeatPublicId()));

            String showPublicId = resolveShowPublicId(seat.getShowId());

            SeatStateEvent event = SeatStateEvent.builder()
                    .showPublicId(showPublicId)
                    .seatPublicId(seat.getSeatPublicId())
                    .state(ShowSeatState.AVAILABLE.getLabel())
                    .eventType("SEAT_RELEASED")
                    .timestamp(LocalDateTime.now())
                    .build();

            webSocketService.broadcastSeatUpdate(showPublicId, event);
        }
    }


    // ======================================================================================
    // UTILITY — RESOLVE SHOW PUBLIC ID
    // ======================================================================================
    private String resolveShowPublicId(Long showId) {
        return showRepository.findById(showId)
                .map(Show::getPublicId)
                .orElse(null);
    }
}
