package com.telecamnig.cinemapos.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.telecamnig.cinemapos.entity.Show;
import com.telecamnig.cinemapos.repository.ShowRepository;
import com.telecamnig.cinemapos.service.ShowService;
import com.telecamnig.cinemapos.utility.Constants.ShowStatus;

@Component
public class ShowStatusScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(ShowStatusScheduler.class);
    
    @Autowired
    private ShowRepository showRepository;
    
    @Autowired
    private ShowService showService;
    
    /**
     * =====================================================================
     * AUTO SHOW STATUS UPDATE SCHEDULER
     * =====================================================================
     * 
     * PURPOSE:
     * Automatically updates show statuses based on real-time clock:
     * 1. SCHEDULED (0) → RUNNING (1) when show's start time is reached/passed
     * 2. RUNNING (1) → COMPLETED (2) when show's end time has passed
     * 
     * SCHEDULE:
     * - Runs every minute at second 0 (CRON: "0 * * * * *")
     * - Example execution times: 14:00:00, 14:01:00, 14:02:00, etc.
     * 
     * BUSINESS LOGIC:
     * - Ensures shows reflect correct operational status without manual intervention
     * - Critical for counter staff to see accurate show status in real-time
     * - WebSocket broadcasts notify all POS systems immediately
     * 
     * DATABASE QUERIES USED:
     * 1. showRepository.findScheduledShowsToStart(now) 
     *    - Finds SCHEDULED shows with start_time <= current_time
     *    - Query: "SELECT s FROM Show s WHERE s.status = 0 AND s.startAt <= :currentTime"
     * 
     * 2. showRepository.findRunningShows(now)
     *    - Finds RUNNING shows where current_time is between start_time and end_time
     *    - Query: "SELECT s FROM Show s WHERE s.status = 1 AND s.startAt <= :currentTime AND s.endAt > :currentTime"
     * 
     * ERROR HANDLING:
     * - Individual show failures don't stop entire scheduler
     * - Comprehensive logging for troubleshooting
     * - WebSocket failures logged but don't prevent status updates
     * 
     * PERFORMANCE:
     * - Only queries shows that need status updates
     * - Transactional to ensure data consistency
     * - Minimal database impact (milliseconds per run)
     * =====================================================================
     */
    @Scheduled(cron = "0 * * * * *") // Every minute at second 0
    @Transactional
    public void autoUpdateShowStatuses() {
        LocalDateTime now = LocalDateTime.now();
        
        log.info("🚀 [SCHEDULER] Auto show status update started at {}", now);
        
        int startedShowsCount = 0;
        int completedShowsCount = 0;
        
        try {
            // ==================== PART 1: SCHEDULED → RUNNING ====================
            // Purpose: Update shows that should have started by now
            log.debug("[SCHEDULER] Phase 1: Checking for SCHEDULED shows to start...");
            
            // Query: Find all SCHEDULED shows whose start time has been reached
            List<Show> scheduledShows = showRepository.findScheduledShowsToStart(now);
            
            if (scheduledShows.isEmpty()) {
                log.debug("[SCHEDULER] No SCHEDULED shows need to start at {}", now);
            } else {
                log.info("[SCHEDULER] Found {} SCHEDULED show(s) ready to start", scheduledShows.size());
                
                for (Show show : scheduledShows) {
                    try {
                        // Log the show being updated
                        log.info("[SHOW START] Updating show {} (Movie: {}, Screen: {}) - Scheduled start: {}", 
                                show.getPublicId(), 
                                show.getMoviePublicId(),
                                show.getScreenPublicId(),
                                show.getStartAt());
                        
                        // Record old status for WebSocket broadcast
                        Integer oldStatus = show.getStatus();
                        
                        // Update status: SCHEDULED (0) → RUNNING (1)
                        show.setStatus(ShowStatus.RUNNING.getCode());
                        showRepository.save(show);
                        startedShowsCount++;
                        
                        // Broadcast status change to all connected POS systems via WebSocket
                        // This ensures counter staff see real-time updates
//                        showService.broadcastShowStatusUpdate(
//                            show.getPublicId(), 
//                            oldStatus, 
//                            ShowStatus.RUNNING.getCode()
//                        );
                        
                        log.info("[SHOW STARTED] ✅ Show {} successfully updated to RUNNING status", 
                                show.getPublicId());
                        
                    } catch (Exception e) {
                        // Individual show failure doesn't stop entire scheduler
                        log.error("[SHOW START ERROR] ❌ Failed to update show {}: {}", 
                                show.getPublicId(), e.getMessage(), e);
                    }
                }
            }
            
            // ==================== PART 2: RUNNING → COMPLETED ====================
            // Purpose: Update shows that have finished running
            log.debug("[SCHEDULER] Phase 2: Checking for RUNNING shows to complete...");
            
            // Query: Find all currently RUNNING shows
            List<Show> endedShows = showRepository.findRunningShowsThatHaveEnded(now);
            
            if (!endedShows.isEmpty()) {
                log.info("[SCHEDULER] Found {} RUNNING show(s) that have ended", endedShows.size());
                
                for (Show show : endedShows) {
                    try {
                        Integer oldStatus = show.getStatus();
                        show.setStatus(ShowStatus.COMPLETED.getCode());
                        showRepository.save(show);
                        completedShowsCount++;
                        
//                        showService.broadcastShowStatusUpdate(
//                            show.getPublicId(), 
//                            oldStatus, 
//                            ShowStatus.COMPLETED.getCode()
//                        );
                        
                        log.info("[SHOW COMPLETED] ✅ Show {} completed (ended at: {})", 
                                show.getPublicId(), show.getEndAt());
                    } catch (Exception e) {
                        log.error("[SHOW COMPLETE ERROR] ❌ Failed to update show {}: {}", 
                                show.getPublicId(), e.getMessage());
                    }
                }
            }
           
            // ==================== FINAL SUMMARY ====================
            // Purpose: Log execution summary for monitoring and auditing
            if (startedShowsCount > 0 || completedShowsCount > 0) {
                log.info("[SCHEDULER SUMMARY] 📊 Execution completed at {}: {} show(s) started, {} show(s) completed", 
                        now, startedShowsCount, completedShowsCount);
            } else {
                log.debug("[SCHEDULER SUMMARY] No status changes required at {}", now);
            }
            
        } catch (Exception e) {
            // Catch any unexpected errors at scheduler level
            log.error("[SCHEDULER CRITICAL ERROR] 💥 Scheduler execution failed at {}: {}", 
                    now, e.getMessage(), e);
        }
    }
}