package com.telecamnig.cinemapos.service;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;

import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.ScheduleShowRequest;
import com.telecamnig.cinemapos.dto.ShowResponse;
import com.telecamnig.cinemapos.dto.ShowSearchRequest;
import com.telecamnig.cinemapos.dto.ShowWithSeatsResponse;
import com.telecamnig.cinemapos.dto.ShowsListResponse;
import com.telecamnig.cinemapos.dto.UpdateShowStatusRequest;
import com.telecamnig.cinemapos.entity.Show;

/**
 * Show Service interface with WebSocket integration for real-time updates.
 * Now includes WebSocket broadcasting for show lifecycle events.
 */
public interface ShowService {
    
    // Existing method
    ResponseEntity<ShowResponse> scheduleShow(ScheduleShowRequest request);
    
    // New methods for Phase 1
    ResponseEntity<ShowWithSeatsResponse> getShowDetails(String showPublicId);
    
    ResponseEntity<ShowWithSeatsResponse> getShowSeats(String showPublicId);
    
    ResponseEntity<ShowWithSeatsResponse> getShowSeatMap(String showPublicId);
    
    // Phase 2 methods - Show Listing APIs
    ResponseEntity<ShowsListResponse> getUpcomingShows(int page, int size);
    
    ResponseEntity<ShowsListResponse> getRunningShows();
    
    ResponseEntity<ShowsListResponse> getActiveShows(int page, int size);
    
    ResponseEntity<ShowsListResponse> getShowHistory(int page, int size);
    
    // Phase 3 methods - Filtered Search & Admin Management APIs
    ResponseEntity<ShowsListResponse> searchShows(ShowSearchRequest request);
    
    ResponseEntity<ShowsListResponse> getShowsByMovie(String moviePublicId, int page, int size);
    
    ResponseEntity<ShowsListResponse> getShowsByScreen(String screenPublicId, int page, int size);
    
    ResponseEntity<ShowsListResponse> getShowsByDate(LocalDate date, int page, int size);
    
    ResponseEntity<CommonApiResponse> updateShowStatus(String showPublicId, UpdateShowStatusRequest request);
    
    ResponseEntity<CommonApiResponse> cancelShow(String showPublicId, UpdateShowStatusRequest request);
    
    ResponseEntity<ShowsListResponse> getAllShows(int page, int size);
    
    void broadcastShowCreated(Show show);
    
    void broadcastShowStatusUpdate(String showPublicId, Integer oldStatus, Integer newStatus);
    
    void broadcastShowCancelled(String showPublicId, String reason);

}