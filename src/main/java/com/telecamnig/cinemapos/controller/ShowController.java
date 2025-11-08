package com.telecamnig.cinemapos.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.ScheduleShowRequest;
import com.telecamnig.cinemapos.dto.ShowResponse;
import com.telecamnig.cinemapos.dto.ShowSearchRequest;
import com.telecamnig.cinemapos.dto.ShowWithSeatsResponse;
import com.telecamnig.cinemapos.dto.ShowsListResponse;
import com.telecamnig.cinemapos.dto.UpdateShowStatusRequest;
import com.telecamnig.cinemapos.service.ShowService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shows")
public class ShowController {

	private final ShowService showService;

	public ShowController(ShowService showService) {
		this.showService = showService;
	}

	/**
	 * Schedule a new show (Admin only) POST /api/v1/shows/schedule
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/schedule")
	public ResponseEntity<ShowResponse> scheduleShow(@Valid @RequestBody ScheduleShowRequest request) {
	    return showService.scheduleShow(request);
	}
	
	/**
     * GET /api/v1/shows/{showPublicId}
     * 
     * Retrieves complete details of a specific show including movie information,
     * screen details, and seat availability summary.
     * 
     * This API is used for:
     * - Show detail pages
     * - Admin show management
     * - Counter staff show overview
     * 
     * Authentication: Required (All authenticated users)
     * 
     * @param showPublicId The public UUID of the show to retrieve
     * @return ShowWithSeatsResponse containing show details and seat summary
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{showPublicId}")
    public ResponseEntity<ShowWithSeatsResponse> getShowDetails(
            @PathVariable("showPublicId") String showPublicId) {
        return showService.getShowDetails(showPublicId);
    }

    /**
     * GET /api/v1/shows/{showPublicId}/seats
     * 
     * Retrieves detailed seat information for a specific show including
     * individual seat states (AVAILABLE/HELD/SOLD), prices, and positions.
     * 
     * This API is optimized for:
     * - Seat selection UI
     * - Real-time seat availability
     * - Booking flow
     * 
     * The response includes parsed layout metadata for frontend rendering.
     * 
     * Authentication: Required (All authenticated users)
     * 
     * @param showPublicId The public UUID of the show
     * @return ShowWithSeatsResponse with complete seat list and layout data
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{showPublicId}/seats")
    public ResponseEntity<ShowWithSeatsResponse> getShowSeats(
            @PathVariable("showPublicId") String showPublicId) {
        return showService.getShowSeats(showPublicId);
    }

    /**
     * GET /api/v1/shows/{showPublicId}/seat-map
     * 
     * Retrieves seat map information optimized for visual rendering.
     * Includes screen layout structure and seat positions for canvas-based UI.
     * 
     * This API is specifically designed for:
     * - Cinema seat map visualization
     * - Interactive seat selection
     * - Real-time seat status updates
     * 
     * Returns both the screen layout and individual seat coordinates.
     * 
     * Authentication: Required (All authenticated users)
     * 
     * @param showPublicId The public UUID of the show
     * @return ShowWithSeatsResponse with seat map data
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{showPublicId}/seat-map")
    public ResponseEntity<ShowWithSeatsResponse> getShowSeatMap(
            @PathVariable("showPublicId") String showPublicId) {
        return showService.getShowSeatMap(showPublicId);
    }
    
    /**
     * GET /api/v1/shows/upcoming
     * 
     * Retrieves all upcoming shows (scheduled but not yet started).
     * This API is primarily used by:
     * - Counter staff for upcoming show management
     * - Admin dashboard for scheduling overview
     * - Future: Customer-facing show timings
     * 
     * Shows are filtered to include only SCHEDULED status with start time in future.
     * Results are ordered by start time (ascending) for easy viewing.
     * 
     * Supports pagination for large datasets.
     * 
     * Authentication: Required (All authenticated users)
     * 
     * @param page Page number for pagination (default: 0)
     * @param size Page size for pagination (default: 20)
     * @return ShowsListResponse with list of upcoming shows
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/upcoming")
    public ResponseEntity<ShowsListResponse> getUpcomingShows(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return showService.getUpcomingShows(page, size);
    }

    /**
     * GET /api/v1/shows/running
     * 
     * Retrieves all currently running shows (started but not yet completed).
     * This API is used by:
     * - Counter staff for active show monitoring
     * - Admin for real-time operations
     * - Concession staff for peak time analysis
     * 
     * Shows are filtered to include only RUNNING status with:
     * - Start time <= current time
     * - End time > current time
     * 
     * Results are ordered by screen for easy staff assignment.
     * 
     * Authentication: Required (All authenticated users)
     * 
     * @return ShowsListResponse with list of running shows
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/running")
    public ResponseEntity<ShowsListResponse> getRunningShows() {
        return showService.getRunningShows();
    }

    /**
     * GET /api/v1/shows/active
     * 
     * Retrieves all active shows (both upcoming and running).
     * This is the primary API for counter staff dashboard showing:
     * - Shows that are currently running
     * - Shows that will start soon
     * 
     * Combines results from upcoming and running shows for comprehensive view.
     * Essential for daily operations and staff scheduling.
     * 
     * Authentication: Required (All authenticated users)
     * 
     * @param page Page number for pagination (default: 0)
     * @param size Page size for pagination (default: 50)
     * @return ShowsListResponse with list of active shows
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/active")
    public ResponseEntity<ShowsListResponse> getActiveShows(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return showService.getActiveShows(page, size);
    }

    /**
     * GET /api/v1/shows/history
     * 
     * Retrieves completed and cancelled shows (historical data).
     * This API is primarily for:
     * - Admin reporting and analytics
     * - Historical performance analysis
     * - Audit and compliance requirements
     * 
     * Shows are filtered to include COMPLETED and CANCELLED status.
     * Results are ordered by start time (descending) for recent first.
     * 
     * Admin only access due to sensitive historical data.
     * 
     * Authentication: Required (Admin role only)
     * 
     * @param page Page number for pagination (default: 0)
     * @param size Page size for pagination (default: 20)
     * @return ShowsListResponse with list of historical shows
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/history")
    public ResponseEntity<ShowsListResponse> getShowHistory(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return showService.getShowHistory(page, size);
    }
    
 // ==================== FILTERED SEARCH APIS ====================

    /**
     * GET /api/v1/shows/search
     * 
     * Advanced search for shows with multiple filter criteria.
     * This API provides flexible searching for:
     * - Admin reporting and analytics
     * - Counter staff looking for specific shows
     * - Operational planning and scheduling
     * 
     * Supports filtering by:
     * - Movie (specific movie)
     * - Screen (specific screen) 
     * - Date (specific date or date range)
     * - Status (show status)
     * 
     * All filters are optional - omitting a filter includes all values.
     * Results are paginated for large datasets.
     * 
     * Authentication: Required (All authenticated users)
     * 
     * @param request ShowSearchRequest with filter criteria
     * @return ShowsListResponse with filtered shows
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    public ResponseEntity<ShowsListResponse> searchShows(@Valid ShowSearchRequest request) {
        return showService.searchShows(request);
    }

    /**
     * GET /api/v1/shows/movie/{moviePublicId}
     * 
     * Retrieves all shows for a specific movie.
     * Useful for:
     * - Movie performance analysis
     * - Customer inquiries about show times
     * - Admin movie scheduling review
     * 
     * Returns both active and historical shows for the specified movie.
     * Results include show times across all screens.
     * 
     * Authentication: Required (All authenticated users)
     * 
     * @param moviePublicId The public UUID of the movie
     * @param page Page number for pagination (default: 0)
     * @param size Page size for pagination (default: 20)
     * @return ShowsListResponse with shows for the specified movie
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/movie/{moviePublicId}")
    public ResponseEntity<ShowsListResponse> getShowsByMovie(
            @PathVariable("moviePublicId") String moviePublicId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return showService.getShowsByMovie(moviePublicId, page, size);
    }

    /**
     * GET /api/v1/shows/screen/{screenPublicId}
     * 
     * Retrieves all shows for a specific screen.
     * Useful for:
     * - Screen utilization analysis
     * - Maintenance scheduling
     * - Staff assignment planning
     * 
     * Returns shows across all time periods for the specified screen.
     * Includes both scheduled and historical shows.
     * 
     * Authentication: Required (All authenticated users)
     * 
     * @param screenPublicId The public UUID of the screen
     * @param page Page number for pagination (default: 0)
     * @param size Page size for pagination (default: 20)
     * @return ShowsListResponse with shows for the specified screen
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/screen/{screenPublicId}")
    public ResponseEntity<ShowsListResponse> getShowsByScreen(
            @PathVariable("screenPublicId") String screenPublicId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return showService.getShowsByScreen(screenPublicId, page, size);
    }

    /**
     * GET /api/v1/shows/date/{date}
     * 
     * Retrieves all shows for a specific date.
     * Essential for:
     * - Daily operations planning
     * - Staff scheduling
     * - Concession inventory planning
     * 
     * Returns all shows scheduled for the specified date across all screens.
     * Includes shows that start on the specified date.
     * 
     * Authentication: Required (All authenticated users)
     * 
     * @param date The date to search for (format: YYYY-MM-DD)
     * @param page Page number for pagination (default: 0)
     * @param size Page size for pagination (default: 50)
     * @return ShowsListResponse with shows for the specified date
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/date/{date}")
    public ResponseEntity<ShowsListResponse> getShowsByDate(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return showService.getShowsByDate(date, page, size);
    }

    // ==================== ADMIN MANAGEMENT APIS ====================

    /**
     * PUT /api/v1/shows/{showPublicId}/status
     * 
     * Updates the status of a specific show.
     * Used for manual status management:
     * - Mark show as RUNNING when it starts
     * - Mark show as COMPLETED when it ends
     * - Cancel shows when necessary
     * 
     * Only ADMIN users can modify show status to ensure proper control.
     * Status changes are logged for audit purposes.
     * 
     * Authentication: Required (Admin role only)
     * 
     * @param showPublicId The public UUID of the show to update
     * @param request UpdateShowStatusRequest with new status
     * @return CommonApiResponse indicating success or failure
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{showPublicId}/status")
    public ResponseEntity<CommonApiResponse> updateShowStatus(
            @PathVariable("showPublicId") String showPublicId,
            @Valid @RequestBody UpdateShowStatusRequest request) {
        return showService.updateShowStatus(showPublicId, request);
    }

    /**
     * PUT /api/v1/shows/{showPublicId}/cancel
     * 
     * Cancels a specific show.
     * Specialized endpoint for show cancellations with additional validation:
     * - Prevents cancellation of running or completed shows
     * - Ensures proper refund processing for booked tickets
     * - Sends notifications to affected customers
     * 
     * Only ADMIN users can cancel shows.
     * Cancellation reason is required for audit purposes.
     * 
     * Authentication: Required (Admin role only)
     * 
     * @param showPublicId The public UUID of the show to cancel
     * @param request UpdateShowStatusRequest with cancellation reason
     * @return CommonApiResponse indicating success or failure
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{showPublicId}/cancel")
    public ResponseEntity<CommonApiResponse> cancelShow(
            @PathVariable("showPublicId") String showPublicId,
            @Valid @RequestBody UpdateShowStatusRequest request) {
        return showService.cancelShow(showPublicId, request);
    }

    /**
     * GET /api/v1/shows/admin/all
     * 
     * Retrieves all shows with comprehensive administrative view.
     * Provides complete show data for:
     * - Admin dashboard and reporting
     * - System-wide show management
     * - Audit and compliance reviews
     * 
     * Includes all show statuses (active, historical, cancelled).
     * Comprehensive pagination for large datasets.
     * 
     * Authentication: Required (Admin role only)
     * 
     * @param page Page number for pagination (default: 0)
     * @param size Page size for pagination (default: 50)
     * @return ShowsListResponse with all shows
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<ShowsListResponse> getAllShows(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return showService.getAllShows(page, size);
    }
	
}