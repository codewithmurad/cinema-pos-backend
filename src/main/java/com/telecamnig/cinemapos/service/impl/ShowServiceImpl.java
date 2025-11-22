package com.telecamnig.cinemapos.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.MovieDto;
import com.telecamnig.cinemapos.dto.ScheduleShowRequest;
import com.telecamnig.cinemapos.dto.ScreenDto;
import com.telecamnig.cinemapos.dto.SeatMapSummaryDto;
import com.telecamnig.cinemapos.dto.SeatStateEvent;
import com.telecamnig.cinemapos.dto.ShowDetailDto;
import com.telecamnig.cinemapos.dto.ShowDto;
import com.telecamnig.cinemapos.dto.ShowListItemDto;
import com.telecamnig.cinemapos.dto.ShowResponse;
import com.telecamnig.cinemapos.dto.ShowSearchRequest;
import com.telecamnig.cinemapos.dto.ShowSeatDto;
import com.telecamnig.cinemapos.dto.ShowWithSeatsResponse;
import com.telecamnig.cinemapos.dto.ShowsListResponse;
import com.telecamnig.cinemapos.dto.ShowsSummaryDto;
import com.telecamnig.cinemapos.dto.SystemEvent;
import com.telecamnig.cinemapos.dto.UpdateShowStatusRequest;
import com.telecamnig.cinemapos.entity.Movie;
import com.telecamnig.cinemapos.entity.Screen;
import com.telecamnig.cinemapos.entity.ScreenSeat;
import com.telecamnig.cinemapos.entity.Show;
import com.telecamnig.cinemapos.entity.ShowSeat;
import com.telecamnig.cinemapos.exception.InvalidSeatTypeException;
import com.telecamnig.cinemapos.exception.ShowConflictException;
import com.telecamnig.cinemapos.repository.MovieRepository;
import com.telecamnig.cinemapos.repository.ScreenRepository;
import com.telecamnig.cinemapos.repository.ScreenSeatRepository;
import com.telecamnig.cinemapos.repository.ShowRepository;
import com.telecamnig.cinemapos.repository.ShowSeatRepository;
import com.telecamnig.cinemapos.service.ShowService;
import com.telecamnig.cinemapos.utility.ApiResponseMessage;
import com.telecamnig.cinemapos.utility.Constants.MovieStatus;
import com.telecamnig.cinemapos.utility.Constants.ScreenStatus;
import com.telecamnig.cinemapos.utility.Constants.SeatStatus;
import com.telecamnig.cinemapos.utility.Constants.SeatType;
import com.telecamnig.cinemapos.utility.Constants.ShowSeatState;
import com.telecamnig.cinemapos.utility.Constants.ShowStatus;
import com.telecamnig.cinemapos.utility.Constants.SystemEventAction;
import com.telecamnig.cinemapos.utility.Constants.SystemEventType;
import com.telecamnig.cinemapos.websocket.WebSocketService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class ShowServiceImpl implements ShowService {

    private static final Logger log = LoggerFactory.getLogger(ShowServiceImpl.class);

    public static final int SHOW_BUFFER_MINUTES = 30;
    
    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final ScreenSeatRepository screenSeatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final Validator validator;
    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;

    public ShowServiceImpl(ShowRepository showRepository, MovieRepository movieRepository,
                          ScreenRepository screenRepository, ScreenSeatRepository screenSeatRepository,
                          ShowSeatRepository showSeatRepository, Validator validator, ObjectMapper objectMapper,
                          WebSocketService webSocketService) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.screenRepository = screenRepository;
        this.screenSeatRepository = screenSeatRepository;
        this.showSeatRepository = showSeatRepository;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.webSocketService = webSocketService;
    }

    @Override
    @Transactional
    public ResponseEntity<ShowResponse> scheduleShow(ScheduleShowRequest request) {
        
        // 🔒 Step 1: Authentication check
//        Authentication auth = getAuthenticatedUser();
//        if (auth == null) {
//            log.warn("Unauthorized access attempt to scheduleShow()");
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ShowResponse.builder()
//                            .success(false)
//                            .message(ApiResponseMessage.UNAUTHORIZED_ACCESS)
//                            .build());
//        }

        try {
            // Step 2: Validate request DTO
            var violations = validator.validate(request);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<ScheduleShowRequest> v : violations) {
                    sb.append(v.getPropertyPath()).append("=").append(v.getMessage()).append("; ");
                }
                log.warn("ScheduleShow request validation failed: {}", sb.toString());
                throw new ConstraintViolationException(violations);
            }

            // Step 3: Validate seat types from request
            validateSeatTypes(request.getSeatPrices());

            // Step 4: Fetch and validate movie
            Movie movie = movieRepository.findByPublicId(request.getMoviePublicId())
                    .orElseThrow(() -> {
                        log.warn("Movie not found for publicId: {}", request.getMoviePublicId());
                        return new IllegalArgumentException(ApiResponseMessage.MOVIE_NOT_FOUND);
                    });

            // Use MovieStatus enum for validation
            if (movie.getStatus() != MovieStatus.ACTIVE.getCode()) {
                log.warn("Movie is not active: {}", request.getMoviePublicId());
                return ResponseEntity.badRequest()
                        .body(ShowResponse.builder()
                                .success(false)
                                .message(ApiResponseMessage.MOVIE_NOT_ACTIVE)
                                .build());
            }

            // Step 5: Fetch and validate screen
            Screen screen = screenRepository.findByPublicId(request.getScreenPublicId())
                    .orElseThrow(() -> {
                        log.warn("Screen not found for publicId: {}", request.getScreenPublicId());
                        return new IllegalArgumentException(ApiResponseMessage.SCREEN_NOT_FOUND);
                    });

            // Use ScreenStatus enum for validation
            if (screen.getStatus() != ScreenStatus.ACTIVE.getCode()) {
                log.warn("Screen is not active: {}", request.getScreenPublicId());
                return ResponseEntity.badRequest()
                        .body(ShowResponse.builder()
                                .success(false)
                                .message(ApiResponseMessage.SCREEN_NOT_ACTIVE)
                                .build());
            }

            // Step 6: Calculate end time
            LocalDateTime endAt = calculateEndTime(request.getStartAt(), movie.getDurationMinutes());

            // Step 7: Check for time conflicts (only check ACTIVE and SCHEDULED shows)
            checkTimeConflicts(screen.getId(), request.getStartAt(), endAt);

            // Step 8: Create and save Show
            Show show = createShowEntity(request, movie, screen, endAt);
            Show savedShow = showRepository.save(show);
            log.info("Show created: id={}, publicId={}", savedShow.getId(), savedShow.getPublicId());

            // Step 8.5: BROADCAST SHOW CREATION VIA WEB SOCKET
            broadcastShowCreated(savedShow);
            
            // Step 9: Generate ShowSeats
            generateShowSeats(savedShow, screen.getId(), request.getSeatPrices());

            // Step 10: Build success response
            ShowDto showDto = mapToShowDto(savedShow);
            String successMessage = ApiResponseMessage.SHOW_SCHEDULED_SUCCESS + " publicId:" + savedShow.getPublicId();
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ShowResponse.builder()
                            .success(true)
                            .message(successMessage)
                            .show(showDto)
                            .build());

        } catch (ConstraintViolationException e) {
            throw e; // Re-throw for global exception handler
        } catch (ShowConflictException e) {
            log.warn("Show scheduling conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ShowResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (InvalidSeatTypeException e) {
            log.warn("Invalid seat type: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ShowResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ShowResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error scheduling show", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ShowResponse.builder()
                            .success(false)
                            .message(ApiResponseMessage.SHOW_SCHEDULING_FAILED)
                            .build());
        }
    }

    // ==================== PHASE 1: SHOW DETAILS & SEAT MAP APIS ====================

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ShowWithSeatsResponse> getShowDetails(String showPublicId) {
        /**
         * Retrieves complete show details including movie, screen information and seat summary.
         * Used for show overview pages and admin management.
         */
        
        // 🔒 Step 1: Authentication check
//        Authentication auth = getAuthenticatedUser();
//        if (auth == null) {
//            log.warn("Unauthorized access attempt to getShowDetails for show: {}", showPublicId);
//            return buildUnauthorizedResponse();
//        }

        try {
            // Step 2: Validate input
            if (showPublicId == null || showPublicId.trim().isEmpty()) {
                log.warn("Empty showPublicId provided to getShowDetails");
                return buildBadRequestResponse(ApiResponseMessage.INVALID_SHOW_ID);
            }

            // Step 3: Fetch show with validation
            Show show = showRepository.findByPublicId(showPublicId)
                    .orElseThrow(() -> {
                        log.warn("Show not found for publicId: {}", showPublicId);
                        return new IllegalArgumentException(ApiResponseMessage.SHOW_NOT_FOUND);
                    });

            // Step 4: Fetch related entities
            Movie movie = movieRepository.findById(show.getMovieId())
                    .orElseThrow(() -> {
                        log.warn("Movie not found for show: {}, movieId: {}", showPublicId, show.getMovieId());
                        return new IllegalArgumentException(ApiResponseMessage.MOVIE_NOT_FOUND);
                    });

            Screen screen = screenRepository.findById(show.getScreenId())
                    .orElseThrow(() -> {
                        log.warn("Screen not found for show: {}, screenId: {}", showPublicId, show.getScreenId());
                        return new IllegalArgumentException(ApiResponseMessage.SCREEN_NOT_FOUND);
                    });

            // Step 5: Get seat summary statistics
            SeatMapSummaryDto seatSummary = calculateSeatSummary(show.getId());

            // Step 6: Build detailed response
            ShowDetailDto showDetail = buildShowDetailDto(show, movie, screen, seatSummary);
            
            log.info("Successfully retrieved show details for: {}", showPublicId);
            
            return ResponseEntity.ok(ShowWithSeatsResponse.builder()
                    .success(true)
                    .message(ApiResponseMessage.FETCH_SUCCESS)
                    .show(showDetail)
                    .seatSummary(seatSummary)
                    .build());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument in getShowDetails: {}", e.getMessage());
            return buildNotFoundResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error retrieving show details for: {}", showPublicId, e);
            return buildInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ShowWithSeatsResponse> getShowSeats(String showPublicId) {
        /**
         * Retrieves detailed seat information for seat selection UI.
         * Includes individual seat states, prices, and positions for booking flow.
         */
        
        // 🔒 Step 1: Authentication check
//        Authentication auth = getAuthenticatedUser();
//        if (auth == null) {
//            log.warn("Unauthorized access attempt to getShowSeats for show: {}", showPublicId);
//            return buildUnauthorizedResponse();
//        }

        try {
            // Step 2: Validate input
            if (showPublicId == null || showPublicId.trim().isEmpty()) {
                log.warn("Empty showPublicId provided to getShowSeats");
                return buildBadRequestResponse(ApiResponseMessage.INVALID_SHOW_ID);
            }

            // Step 3: Fetch show with validation
            Show show = showRepository.findByPublicId(showPublicId)
                    .orElseThrow(() -> {
                        log.warn("Show not found for publicId: {}", showPublicId);
                        return new IllegalArgumentException(ApiResponseMessage.SHOW_NOT_FOUND);
                    });

            // Step 4: Fetch all seats for the show
            List<ShowSeat> showSeats = showSeatRepository.findByShowId(show.getId());
            
            if (showSeats.isEmpty()) {
                log.warn("No seats found for show: {}", showPublicId);
                return buildNotFoundResponse(ApiResponseMessage.NO_SEATS_FOUND);
            }

            // Step 5: Convert to DTOs with parsed layout metadata
            List<ShowSeatDto> seatDtos = showSeats.stream()
                    .map(this::convertToShowSeatDto)
                    .collect(Collectors.toList());

            // Step 6: Build seat summary
            SeatMapSummaryDto seatSummary = calculateSeatSummary(show.getId());

            log.info("Successfully retrieved {} seats for show: {}", seatDtos.size(), showPublicId);
            
            return ResponseEntity.ok(ShowWithSeatsResponse.builder()
                    .success(true)
                    .message(ApiResponseMessage.FETCH_SUCCESS)
                    .seats(seatDtos)
                    .seatSummary(seatSummary)
                    .build());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument in getShowSeats: {}", e.getMessage());
            return buildNotFoundResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error retrieving show seats for: {}", showPublicId, e);
            return buildInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ShowWithSeatsResponse> getShowSeatMap(String showPublicId) {
        /**
         * Retrieves complete seat map information optimized for visual rendering.
         * Includes screen layout structure and individual seat coordinates.
         */
        
        // 🔒 Step 1: Authentication check
//        Authentication auth = getAuthenticatedUser();
//        if (auth == null) {
//            log.warn("Unauthorized access attempt to getShowSeatMap for show: {}", showPublicId);
//            return buildUnauthorizedResponse();
//        }

        try {
            // Step 2: Validate input
            if (showPublicId == null || showPublicId.trim().isEmpty()) {
                log.warn("Empty showPublicId provided to getShowSeatMap");
                return buildBadRequestResponse(ApiResponseMessage.INVALID_SHOW_ID);
            }

            // Step 3: Fetch show with validation
            Show show = showRepository.findByPublicId(showPublicId)
                    .orElseThrow(() -> {
                        log.warn("Show not found for publicId: {}", showPublicId);
                        return new IllegalArgumentException(ApiResponseMessage.SHOW_NOT_FOUND);
                    });

            // Step 4: Fetch screen for layout information
            Screen screen = screenRepository.findById(show.getScreenId())
                    .orElseThrow(() -> {
                        log.warn("Screen not found for show: {}", showPublicId);
                        return new IllegalArgumentException(ApiResponseMessage.SCREEN_NOT_FOUND);
                    });

            // Step 5: Fetch all seats with layout metadata
            List<ShowSeat> showSeats = showSeatRepository.findByShowId(show.getId());
            
            if (showSeats.isEmpty()) {
                log.warn("No seats found for show: {}", showPublicId);
                return buildNotFoundResponse(ApiResponseMessage.NO_SEATS_FOUND);
            }

            // Step 6: Convert to DTOs with enhanced layout information
            List<ShowSeatDto> seatDtos = showSeats.stream()
                    .map(this::convertToShowSeatDto)
                    .collect(Collectors.toList());

            // Step 7: Build complete response with screen layout
            ShowDetailDto showDetail = buildBasicShowDetailDto(show);
            SeatMapSummaryDto seatSummary = calculateSeatSummary(show.getId());

            log.info("Successfully retrieved seat map with {} seats for show: {}", seatDtos.size(), showPublicId);
            
            return ResponseEntity.ok(ShowWithSeatsResponse.builder()
                    .success(true)
                    .message(ApiResponseMessage.FETCH_SUCCESS)
                    .show(showDetail)
                    .seats(seatDtos)
                    .seatSummary(seatSummary)
                    .build());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument in getShowSeatMap: {}", e.getMessage());
            return buildNotFoundResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error retrieving seat map for: {}", showPublicId, e);
            return buildInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
        }
    }
    // Add these methods to your existing ShowServiceImpl class

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ShowsListResponse> getUpcomingShows(
            int page,
            int size,
            String moviePublicId,
            LocalDate showDate
    ) {
        // 🔒 Step 1: (optional) authentication
        // Authentication auth = getAuthenticatedUser();
        // if (auth == null) {
        //     log.warn("Unauthorized access attempt to getUpcomingShows");
        //     return buildShowsUnauthorizedResponse();
        // }

        try {
            // Step 2: Validate pagination parameters
            if (page < 0) {
                log.warn("Invalid page number provided: {}", page);
                return buildShowsBadRequestResponse("Page number cannot be negative");
            }
            if (size <= 0 || size > 100) {
                log.warn("Invalid page size provided: {}", size);
                return buildShowsBadRequestResponse("Page size must be between 1 and 100");
            }

            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by("startAt").ascending()
            );

            // Step 3: Build time range filters
            //
            // We support three modes:
            // 1) No date filter → from = now, to = null (all future shows)
            // 2) Date only      → from/to are that entire day (but if today, from = now)
            // 3) Movie + date   → same as (2) but restricted to the movie
            //
            // Additionally, showDate must be TODAY or FUTURE. Past dates are rejected.
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();

            LocalDateTime from;
            LocalDateTime to = null; // null means "no upper bound"

            if (showDate != null) {
                // Prevent searching for past dates
                if (showDate.isBefore(today)) {
                    log.warn("Rejected upcoming shows search for past date: {}", showDate);
                    return buildShowsBadRequestResponse("Show date must be today or a future date");
                }

                // Start and end of the requested day
                LocalDateTime startOfDay = showDate.atStartOfDay();
                LocalDateTime startOfNextDay = showDate.plusDays(1).atStartOfDay();

                // If searching today: don't include shows that already started in the past
                if (showDate.isEqual(today)) {
                    from = now;
                } else {
                    from = startOfDay;
                }
                to = startOfNextDay; // exclusive upper bound
            } else {
                // No date filter → from now onwards
                from = now;
            }

            // Step 4: Resolve optional movie filter
            //
            // If moviePublicId is provided, we resolve it to internal movieId.
            // If the publicId is invalid, we return 400 (client error).
            Long movieId = null;
            if (moviePublicId != null && !moviePublicId.isBlank()) {
                String trimmed = moviePublicId.trim();
                Optional<Movie> movieOpt = movieRepository.findByPublicId(trimmed);
                if (movieOpt.isEmpty()) {
                    log.warn("Invalid moviePublicId provided: {}", trimmed);
                    return buildShowsBadRequestResponse("Invalid movie reference");
                }	
                movieId = movieOpt.get().getId();
            }

            // Step 5: Execute repository search
            Integer statusScheduled = ShowStatus.SCHEDULED.getCode();

            Page<Show> showsPage = showRepository.searchUpcomingShows(
                    statusScheduled,
                    from,
                    to,
                    movieId,
                    pageable
            );

            // Step 6: Convert entities to lightweight list DTOs
            List<ShowListItemDto> showDtos = showsPage.getContent().stream()
                    .map(this::convertToShowListItemDto)
                    .collect(Collectors.toList());

            // Step 7: Build summary statistics (per screen, per movie, etc.)
            ShowsSummaryDto summary = buildUpcomingShowsSummary(showsPage.getContent());

            log.info(
                    "Retrieved {} upcoming shows (page {} of {}) [moviePublicId={}, showDate={}]",
                    showDtos.size(),
                    page,
                    showsPage.getTotalPages(),
                    moviePublicId,
                    showDate
            );

            // Step 8: Build and return response
            return ResponseEntity.ok(
                    ShowsListResponse.builder()
                            .success(true)
                            .message(ApiResponseMessage.FETCH_SUCCESS)
                            .shows(showDtos)
                            .page(showsPage.getNumber())
                            .size(showsPage.getSize())
                            .totalElements(showsPage.getTotalElements())
                            .totalPages(showsPage.getTotalPages())
                            .summary(summary)
                            .build()
            );

        } catch (Exception e) {
            log.error("Unexpected error retrieving upcoming shows", e);
            return buildShowsInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ShowsListResponse> getRunningShows() {
        /**
         * Retrieves all currently running shows (started but not yet completed).
         * Used by counter staff for active show monitoring and real-time operations.
         */
        
        // 🔒 Step 1: Authentication check
//        Authentication auth = getAuthenticatedUser();
//        if (auth == null) {
//            log.warn("Unauthorized access attempt to getRunningShows");
//            return buildShowsUnauthorizedResponse();
//        }

        try {
            // Step 2: Fetch running shows (RUNNING status with current time within show duration)
            LocalDateTime currentTime = LocalDateTime.now();
            
            List<Show> runningShows = showRepository.findRunningShows(currentTime);

            // Step 3: Convert to DTOs
            List<ShowListItemDto> showDtos = runningShows.stream()
                    .map(this::convertToShowListItemDto)
                    .collect(Collectors.toList());

            // Step 4: Build summary statistics
            ShowsSummaryDto summary = buildRunningShowsSummary(runningShows);

            log.info("Retrieved {} currently running shows", showDtos.size());

            return ResponseEntity.ok(ShowsListResponse.builder()
                    .success(true)
                    .message(ApiResponseMessage.FETCH_SUCCESS)
                    .shows(showDtos)
                    .summary(summary)
                    .build());

        } catch (Exception e) {
            log.error("Unexpected error retrieving running shows", e);
            return buildShowsInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ShowsListResponse> getActiveShows(int page, int size) {
        /**
         * Retrieves all active shows (both upcoming and running).
         * Primary API for counter staff dashboard showing current and soon-to-start shows.
         */
        
        // 🔒 Step 1: Authentication check
//        Authentication auth = getAuthenticatedUser();
//        if (auth == null) {
//            log.warn("Unauthorized access attempt to getActiveShows");
//            return buildShowsUnauthorizedResponse();
//        }

        try {
            // Step 2: Validate pagination parameters
            if (page < 0) {
                log.warn("Invalid page number provided: {}", page);
                return buildShowsBadRequestResponse("Page number cannot be negative");
            }
            if (size <= 0 || size > 100) {
                log.warn("Invalid page size provided: {}", size);
                return buildShowsBadRequestResponse("Page size must be between 1 and 100");
            }

            // Step 3: Create pagination object
            Pageable pageable = PageRequest.of(page, size, Sort.by("startAt").ascending());

            // Step 4: Fetch active shows (SCHEDULED + RUNNING status)
            List<Integer> activeStatuses = Arrays.asList(
                com.telecamnig.cinemapos.utility.Constants.ShowStatus.SCHEDULED.getCode(),
                com.telecamnig.cinemapos.utility.Constants.ShowStatus.RUNNING.getCode()
            );
            
            Page<Show> showsPage = showRepository.findByStatusIn(
                activeStatuses, pageable);

            // Step 5: Convert to DTOs
            List<ShowListItemDto> showDtos = showsPage.getContent().stream()
                    .map(this::convertToShowListItemDto)
                    .collect(Collectors.toList());

            // Step 6: Build comprehensive summary
            ShowsSummaryDto summary = buildActiveShowsSummary(showsPage.getContent());

            log.info("Retrieved {} active shows (page {} of {})", 
                    showDtos.size(), page, showsPage.getTotalPages());

            return ResponseEntity.ok(ShowsListResponse.builder()
                    .success(true)
                    .message(ApiResponseMessage.FETCH_SUCCESS)
                    .shows(showDtos)
                    .page(showsPage.getNumber())
                    .size(showsPage.getSize())
                    .totalElements(showsPage.getTotalElements())
                    .totalPages(showsPage.getTotalPages())
                    .summary(summary)
                    .build());

        } catch (Exception e) {
            log.error("Unexpected error retrieving active shows", e);
            return buildShowsInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ShowsListResponse> getShowHistory(int page, int size) {
        /**
         * Retrieves completed and cancelled shows (historical data).
         * Admin only access for reporting, analytics, and audit purposes.
         */
        
        // 🔒 Step 1: Authentication check (already handled by @PreAuthorize, but double-check)
//        Authentication auth = getAuthenticatedUser();
//        if (auth == null) {
//            log.warn("Unauthorized access attempt to getShowHistory");
//            return buildShowsUnauthorizedResponse();
//        }

        try {
            // Step 2: Validate pagination parameters
            if (page < 0) {
                log.warn("Invalid page number provided: {}", page);
                return buildShowsBadRequestResponse("Page number cannot be negative");
            }
            if (size <= 0 || size > 100) {
                log.warn("Invalid page size provided: {}", size);
                return buildShowsBadRequestResponse("Page size must be between 1 and 100");
            }

            // Step 3: Create pagination object (recent first)
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, 
                    org.springframework.data.domain.Sort.by("startAt").descending());

            // Step 4: Fetch historical shows (COMPLETED + CANCELLED status)
            List<Integer> historyStatuses = Arrays.asList(
                com.telecamnig.cinemapos.utility.Constants.ShowStatus.COMPLETED.getCode(),
                com.telecamnig.cinemapos.utility.Constants.ShowStatus.CANCELLED.getCode()
            );
            
            Page<Show> showsPage = showRepository.findByStatusIn(
                historyStatuses, pageable);

            // Step 5: Convert to DTOs
            List<ShowListItemDto> showDtos = showsPage.getContent().stream()
                    .map(this::convertToShowListItemDto)
                    .collect(Collectors.toList());

            // Step 6: Build historical summary
            ShowsSummaryDto summary = buildHistoryShowsSummary(showsPage.getContent());

            log.info("Retrieved {} historical shows (page {} of {})", 
                    showDtos.size(), page, showsPage.getTotalPages());

            return ResponseEntity.ok(ShowsListResponse.builder()
                    .success(true)
                    .message(ApiResponseMessage.FETCH_SUCCESS)
                    .shows(showDtos)
                    .page(showsPage.getNumber())
                    .size(showsPage.getSize())
                    .totalElements(showsPage.getTotalElements())
                    .totalPages(showsPage.getTotalPages())
                    .summary(summary)
                    .build());

        } catch (Exception e) {
            log.error("Unexpected error retrieving show history", e);
            return buildShowsInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
        }
    }
    

	 // ==================== PHASE 3: FILTERED SEARCH APIS ====================
	
	 @Override
	 @Transactional(readOnly = true)
	 public ResponseEntity<ShowsListResponse> searchShows(ShowSearchRequest request) {
	     /**
	      * Advanced search for shows with multiple filter criteria.
	      * Provides flexible searching with optional filters.
	      */
	     
	     // 🔒 Step 1: Authentication check
//	     Authentication auth = getAuthenticatedUser();
//	     if (auth == null) {
//	         log.warn("Unauthorized access attempt to searchShows");
//	         return buildShowsUnauthorizedResponse();
//	     }
	
	     try {
	         // Step 2: Validate pagination parameters
	         if (request.getPage() < 0) {
	             log.warn("Invalid page number provided: {}", request.getPage());
	             return buildShowsBadRequestResponse("Page number cannot be negative");
	         }
	         if (request.getSize() <= 0 || request.getSize() > 100) {
	             log.warn("Invalid page size provided: {}", request.getSize());
	             return buildShowsBadRequestResponse("Page size must be between 1 and 100");
	         }
	
	         // Step 3: Create pagination object
	         Pageable pageable = 
	             PageRequest.of(request.getPage(), request.getSize(), 
	                 Sort.by("startAt").ascending());
	
	         // Step 4: Build search criteria and execute search
	         Page<Show> showsPage = executeAdvancedSearch(request, pageable);
	
	         // Step 5: Convert to DTOs
	         List<ShowListItemDto> showDtos = showsPage.getContent().stream()
	                 .map(this::convertToShowListItemDto)
	                 .collect(Collectors.toList());
	
	         // Step 6: Build search summary
	         ShowsSummaryDto summary = buildSearchSummary(showsPage.getContent(), request);
	
	         log.info("Search completed: found {} shows with filters: movie={}, screen={}, date={}, status={}", 
	                 showDtos.size(), request.getMoviePublicId(), request.getScreenPublicId(), 
	                 request.getDate(), request.getStatus());
	
	         return ResponseEntity.ok(ShowsListResponse.builder()
	                 .success(true)
	                 .message(ApiResponseMessage.FETCH_SUCCESS)
	                 .shows(showDtos)
	                 .page(showsPage.getNumber())
	                 .size(showsPage.getSize())
	                 .totalElements(showsPage.getTotalElements())
	                 .totalPages(showsPage.getTotalPages())
	                 .summary(summary)
	                 .build());
	
	     } catch (IllegalArgumentException e) {
	         log.warn("Invalid search criteria: {}", e.getMessage());
	         return buildShowsBadRequestResponse(e.getMessage());
	     } catch (Exception e) {
	         log.error("Unexpected error during show search", e);
	         return buildShowsInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
	     }
	 }
	
	 @Override
	 @Transactional(readOnly = true)
	 public ResponseEntity<ShowsListResponse> getShowsByMovie(String moviePublicId, int page, int size) {
	     /**
	      * Retrieves all shows for a specific movie.
	      */
	     
	     // 🔒 Step 1: Authentication check
//	     Authentication auth = getAuthenticatedUser();
//	     if (auth == null) {
//	         log.warn("Unauthorized access attempt to getShowsByMovie for movie: {}", moviePublicId);
//	         return buildShowsUnauthorizedResponse();
//	     }
	
	     try {
	         // Step 2: Validate parameters
	         if (moviePublicId == null || moviePublicId.trim().isEmpty()) {
	             log.warn("Empty moviePublicId provided to getShowsByMovie");
	             return buildShowsBadRequestResponse(ApiResponseMessage.INVALID_INPUT);
	         }
	         if (page < 0) {
	             log.warn("Invalid page number provided: {}", page);
	             return buildShowsBadRequestResponse("Page number cannot be negative");
	         }
	         if (size <= 0 || size > 100) {
	             log.warn("Invalid page size provided: {}", size);
	             return buildShowsBadRequestResponse("Page size must be between 1 and 100");
	         }
	
	         // Step 3: Validate movie exists
	         Movie movie = movieRepository.findByPublicId(moviePublicId)
	                 .orElseThrow(() -> {
	                     log.warn("Movie not found for publicId: {}", moviePublicId);
	                     return new IllegalArgumentException(ApiResponseMessage.MOVIE_NOT_FOUND);
	                 });
	
	         // Step 4: Create pagination object
	         Pageable pageable = 
	             PageRequest.of(page, size, Sort.by("startAt").descending());
	
	         // Step 5: Fetch shows by movie
	         Page<Show> showsPage = showRepository.findByMovieId(movie.getId(), pageable);
	
	         // Step 6: Convert to DTOs
	         List<ShowListItemDto> showDtos = showsPage.getContent().stream()
	                 .map(this::convertToShowListItemDto)
	                 .collect(Collectors.toList());
	
	         // Step 7: Build summary
	         ShowsSummaryDto summary = buildMovieShowsSummary(showsPage.getContent(), movie);
	
	         log.info("Retrieved {} shows for movie: {} (page {} of {})", 
	                 showDtos.size(), movie.getTitle(), page, showsPage.getTotalPages());
	
	         return ResponseEntity.ok(ShowsListResponse.builder()
	                 .success(true)
	                 .message(ApiResponseMessage.FETCH_SUCCESS)
	                 .shows(showDtos)
	                 .page(showsPage.getNumber())
	                 .size(showsPage.getSize())
	                 .totalElements(showsPage.getTotalElements())
	                 .totalPages(showsPage.getTotalPages())
	                 .summary(summary)
	                 .build());
	
	     } catch (IllegalArgumentException e) {
	         log.warn("Invalid argument in getShowsByMovie: {}", e.getMessage());
	         return buildShowsNotFoundResponse(e.getMessage());
	     } catch (Exception e) {
	         log.error("Unexpected error retrieving shows for movie: {}", moviePublicId, e);
	         return buildShowsInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
	     }
	 }
	
	 @Override
	 @Transactional(readOnly = true)
	 public ResponseEntity<ShowsListResponse> getShowsByScreen(String screenPublicId, int page, int size) {
	     /**
	      * Retrieves all shows for a specific screen.
	      */
	     
	     // 🔒 Step 1: Authentication check
//	     Authentication auth = getAuthenticatedUser();
//	     if (auth == null) {
//	         log.warn("Unauthorized access attempt to getShowsByScreen for screen: {}", screenPublicId);
//	         return buildShowsUnauthorizedResponse();
//	     }
	
	     try {
	         // Step 2: Validate parameters
	         if (screenPublicId == null || screenPublicId.trim().isEmpty()) {
	             log.warn("Empty screenPublicId provided to getShowsByScreen");
	             return buildShowsBadRequestResponse(ApiResponseMessage.INVALID_INPUT);
	         }
	         if (page < 0) {
	             log.warn("Invalid page number provided: {}", page);
	             return buildShowsBadRequestResponse("Page number cannot be negative");
	         }
	         if (size <= 0 || size > 100) {
	             log.warn("Invalid page size provided: {}", size);
	             return buildShowsBadRequestResponse("Page size must be between 1 and 100");
	         }
	
	         // Step 3: Validate screen exists
	         Screen screen = screenRepository.findByPublicId(screenPublicId)
	                 .orElseThrow(() -> {
	                     log.warn("Screen not found for publicId: {}", screenPublicId);
	                     return new IllegalArgumentException(ApiResponseMessage.SCREEN_NOT_FOUND);
	                 });
	
	         // Step 4: Create pagination object
	         Pageable pageable = PageRequest.of(page, size, Sort.by("startAt").descending());
	
	         // Step 5: Fetch shows by screen
	         Page<Show> showsPage = showRepository.findByScreenId(
	             screen.getId(), pageable);
	
	         // Step 6: Convert to DTOs
	         List<ShowListItemDto> showDtos = showsPage.getContent().stream()
	                 .map(this::convertToShowListItemDto)
	                 .collect(Collectors.toList());
	
	         // Step 7: Build summary
	         ShowsSummaryDto summary = buildScreenShowsSummary(showsPage.getContent(), screen);
	
	         log.info("Retrieved {} shows for screen: {} (page {} of {})", 
	                 showDtos.size(), screen.getName(), page, showsPage.getTotalPages());
	
	         return ResponseEntity.ok(ShowsListResponse.builder()
	                 .success(true)
	                 .message(ApiResponseMessage.FETCH_SUCCESS)
	                 .shows(showDtos)
	                 .page(showsPage.getNumber())
	                 .size(showsPage.getSize())
	                 .totalElements(showsPage.getTotalElements())
	                 .totalPages(showsPage.getTotalPages())
	                 .summary(summary)
	                 .build());
	
	     } catch (IllegalArgumentException e) {
	         log.warn("Invalid argument in getShowsByScreen: {}", e.getMessage());
	         return buildShowsNotFoundResponse(e.getMessage());
	     } catch (Exception e) {
	         log.error("Unexpected error retrieving shows for screen: {}", screenPublicId, e);
	         return buildShowsInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
	     }
	 }
	
	 @Override
	 @Transactional(readOnly = true)
	 public ResponseEntity<ShowsListResponse> getShowsByDate(LocalDate date, int page, int size) {
	     /**
	      * Retrieves all shows for a specific date.
	      */
	     
	     // 🔒 Step 1: Authentication check
//	     Authentication auth = getAuthenticatedUser();
//	     if (auth == null) {
//	         log.warn("Unauthorized access attempt to getShowsByDate for date: {}", date);
//	         return buildShowsUnauthorizedResponse();
//	     }
	
	     try {
	         // Step 2: Validate parameters
	         if (date == null) {
	             log.warn("Null date provided to getShowsByDate");
	             return buildShowsBadRequestResponse("Date is required");
	         }
	         if (date.isBefore(LocalDate.now().minusYears(1))) {
	             log.warn("Date too far in past: {}", date);
	             return buildShowsBadRequestResponse("Cannot search shows older than 1 year");
	         }
	         if (date.isAfter(LocalDate.now().plusYears(1))) {
	             log.warn("Date too far in future: {}", date);
	             return buildShowsBadRequestResponse("Cannot search shows more than 1 year in future");
	         }
	         if (page < 0) {
	             log.warn("Invalid page number provided: {}", page);
	             return buildShowsBadRequestResponse("Page number cannot be negative");
	         }
	         if (size <= 0 || size > 100) {
	             log.warn("Invalid page size provided: {}", size);
	             return buildShowsBadRequestResponse("Page size must be between 1 and 100");
	         }
	
	         // Step 3: Create pagination object
	         org.springframework.data.domain.Pageable pageable = 
	             org.springframework.data.domain.PageRequest.of(page, size, 
	                 org.springframework.data.domain.Sort.by("startAt").ascending());
	
	         // Step 4: Calculate date range for the day
	         LocalDateTime startOfDay = date.atStartOfDay();
	         LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
	
	         // Step 5: Fetch shows by date range
	         Page<Show> showsPage = showRepository.findByStartAtBetween(
	             startOfDay, endOfDay, pageable);
	
	         // Step 6: Convert to DTOs
	         List<ShowListItemDto> showDtos = showsPage.getContent().stream()
	                 .map(this::convertToShowListItemDto)
	                 .collect(Collectors.toList());
	
	         // Step 7: Build summary
	         ShowsSummaryDto summary = buildDateShowsSummary(showsPage.getContent(), date);
	
	         log.info("Retrieved {} shows for date: {} (page {} of {})", 
	                 showDtos.size(), date, page, showsPage.getTotalPages());
	
	         return ResponseEntity.ok(ShowsListResponse.builder()
	                 .success(true)
	                 .message(ApiResponseMessage.FETCH_SUCCESS)
	                 .shows(showDtos)
	                 .page(showsPage.getNumber())
	                 .size(showsPage.getSize())
	                 .totalElements(showsPage.getTotalElements())
	                 .totalPages(showsPage.getTotalPages())
	                 .summary(summary)
	                 .build());
	
	     } catch (Exception e) {
	         log.error("Unexpected error retrieving shows for date: {}", date, e);
	         return buildShowsInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
	     }
	 }
	
	 // ==================== PHASE 3: ADMIN MANAGEMENT APIS ====================
	
	 @Override
	 @Transactional
	 public ResponseEntity<CommonApiResponse> updateShowStatus(String showPublicId, UpdateShowStatusRequest request) {
	     /**
	      * Updates the status of a specific show.
	      */
	     
	     // 🔒 Step 1: Authentication check (Admin only - handled by controller, but double-check)
//	     Authentication auth = getAuthenticatedUser();
//	     if (auth == null) {
//	         log.warn("Unauthorized access attempt to updateShowStatus for show: {}", showPublicId);
//	         return buildCommonUnauthorizedResponse();
//	     }
	
	     try {
	         // Step 2: Validate input
	         if (showPublicId == null || showPublicId.trim().isEmpty()) {
	             log.warn("Empty showPublicId provided to updateShowStatus");
	             return buildCommonBadRequestResponse(ApiResponseMessage.INVALID_SHOW_ID);
	         }
	
	         // Step 3: Validate status
	         if (request.getStatus() == null) {
	             log.warn("Null status provided to updateShowStatus");
	             return buildCommonBadRequestResponse("Status is required");
	         }
	
	         // Step 4: Validate show status is valid
	         if (!isValidShowStatus(request.getStatus())) {
	             log.warn("Invalid show status provided: {}", request.getStatus());
	             return buildCommonBadRequestResponse("Invalid show status");
	         }
	
	         // Step 5: Fetch show
	         Show show = showRepository.findByPublicId(showPublicId)
	                 .orElseThrow(() -> {
	                     log.warn("Show not found for publicId: {}", showPublicId);
	                     return new IllegalArgumentException(ApiResponseMessage.SHOW_NOT_FOUND);
	                 });
	
	         // Step 6: Check if status is actually changing
	         Integer oldStatus = show.getStatus();
	         
	         if (oldStatus == request.getStatus()) {
	             log.info("Show status unchanged for: {} (already {})", showPublicId, request.getStatus());
	             return ResponseEntity.ok(new CommonApiResponse(true, 
	                 ApiResponseMessage.NO_CHANGE + " - Status already " + request.getStatus()));
	         }
	
	         // Step 7: Validate status transition
	         if (!isValidStatusTransition(show.getStatus(), request.getStatus())) {
	             log.warn("Invalid status transition from {} to {} for show: {}", 
	                     show.getStatus(), request.getStatus(), showPublicId);
	             return buildCommonBadRequestResponse("Invalid status transition");
	         }
	
	         // Step 8: Update show status
	         show.setStatus(request.getStatus());
	         Show updatedShow = showRepository.save(show);
	         
	         // Step 8.5: BROADCAST STATUS UPDATE VIA WEB SOCKET
	         broadcastShowStatusUpdate(showPublicId, oldStatus, request.getStatus());
	
	         log.info("Show status updated: {} -> {} for show: {}", 
	                 show.getStatus(), request.getStatus(), showPublicId);
	
	         return ResponseEntity.ok(new CommonApiResponse(true, 
	             "Show status updated successfully to " + 
	             com.telecamnig.cinemapos.utility.Constants.ShowStatus.fromCode(request.getStatus()).getLabel()));
	
	     } catch (IllegalArgumentException e) {
	         log.warn("Invalid argument in updateShowStatus: {}", e.getMessage());
	         return buildCommonNotFoundResponse(e.getMessage());
	     } catch (Exception e) {
	         log.error("Unexpected error updating show status for: {}", showPublicId, e);
	         return buildCommonInternalErrorResponse("Failed to update show status");
	     }
	 }
	
	 @Override
	 @Transactional
	 public ResponseEntity<CommonApiResponse> cancelShow(String showPublicId, UpdateShowStatusRequest request) {
	     /**
	      * Cancels a specific show with additional validation.
	      */
	     
	     // 🔒 Step 1: Authentication check
//	     Authentication auth = getAuthenticatedUser();
//	     if (auth == null) {
//	         log.warn("Unauthorized access attempt to cancelShow for show: {}", showPublicId);
//	         return buildCommonUnauthorizedResponse();
//	     }
	
	     try {
	         // Step 2: Validate input
	         if (showPublicId == null || showPublicId.trim().isEmpty()) {
	             log.warn("Empty showPublicId provided to cancelShow");
	             return buildCommonBadRequestResponse(ApiResponseMessage.INVALID_SHOW_ID);
	         }
	
	         // Step 3: Validate cancellation reason
	         if (request.getReason() == null || request.getReason().trim().isEmpty()) {
	             log.warn("Cancellation reason required for show: {}", showPublicId);
	             return buildCommonBadRequestResponse("Cancellation reason is required");
	         }
	
	         // Step 4: Fetch show
	         Show show = showRepository.findByPublicId(showPublicId)
	                 .orElseThrow(() -> {
	                     log.warn("Show not found for publicId: {}", showPublicId);
	                     return new IllegalArgumentException(ApiResponseMessage.SHOW_NOT_FOUND);
	                 });
	
	         // Step 5: Check if show can be cancelled
	         if (!canShowBeCancelled(show)) {
	             log.warn("Show cannot be cancelled: {} (status: {})", showPublicId, show.getStatus());
	             return buildCommonBadRequestResponse("Show cannot be cancelled in its current status");
	         }
	
	         // Step 6: Check if show has booked seats
	         long bookedSeatsCount = showSeatRepository.countByShowIdAndState(show.getId(), 
	             ShowSeatState.SOLD.getLabel());
	         
	         if (bookedSeatsCount > 0) {
	             log.warn("Cannot cancel show with booked seats: {} ({} seats)", showPublicId, bookedSeatsCount);
	             return buildCommonBadRequestResponse("Cannot cancel show with confirmed bookings. Refund bookings first.");
	         }
	
	         // Step 7: Update show status to CANCELLED
	         Integer oldStatus = show.getStatus();
	         show.setStatus(com.telecamnig.cinemapos.utility.Constants.ShowStatus.CANCELLED.getCode());
	         Show cancelledShow = showRepository.save(show);
	         
	         // Step 7.5: BROADCAST SHOW CANCELLATION VIA WEB SOCKET
	         broadcastShowCancelled(showPublicId, request.getReason());
	
	         // Step 8: Release any held seats
	         releaseHeldSeatsForShow(show.getId());
	
	         log.info("Show cancelled: {} with reason: {}", showPublicId, request.getReason());
	
	         return ResponseEntity.ok(new CommonApiResponse(true, 
	             "Show cancelled successfully. " + bookedSeatsCount + " held seats released."));
	
	     } catch (IllegalArgumentException e) {
	         log.warn("Invalid argument in cancelShow: {}", e.getMessage());
	         return buildCommonNotFoundResponse(e.getMessage());
	     } catch (Exception e) {
	         log.error("Unexpected error cancelling show: {}", showPublicId, e);
	         return buildCommonInternalErrorResponse("Failed to cancel show");
	     }
	 }
	
	 @Override
	 @Transactional(readOnly = true)
	 public ResponseEntity<ShowsListResponse> getAllShows(int page, int size) {
	     /**
	      * Retrieves all shows with comprehensive administrative view.
	      */
	     
	     // 🔒 Step 1: Authentication check (Admin only)
//	     Authentication auth = getAuthenticatedUser();
//	     if (auth == null) {
//	         log.warn("Unauthorized access attempt to getAllShows");
//	         return buildShowsUnauthorizedResponse();
//	     }
	
	     try {
	         // Step 2: Validate pagination parameters
	         if (page < 0) {
	             log.warn("Invalid page number provided: {}", page);
	             return buildShowsBadRequestResponse("Page number cannot be negative");
	         }
	         if (size <= 0 || size > 100) {
	             log.warn("Invalid page size provided: {}", size);
	             return buildShowsBadRequestResponse("Page size must be between 1 and 100");
	         }
	
	         // Step 3: Create pagination object
	         org.springframework.data.domain.Pageable pageable = 
	             org.springframework.data.domain.PageRequest.of(page, size, 
	                 org.springframework.data.domain.Sort.by("startAt").descending());
	
	         // Step 4: Fetch all shows
	         org.springframework.data.domain.Page<Show> showsPage = showRepository.findAll(pageable);
	
	         // Step 5: Convert to DTOs
	         List<ShowListItemDto> showDtos = showsPage.getContent().stream()
	                 .map(this::convertToShowListItemDto)
	                 .collect(Collectors.toList());
	
	         // Step 6: Build comprehensive summary
	         ShowsSummaryDto summary = buildAllShowsSummary(showsPage.getContent());
	
	         log.info("Admin retrieved all {} shows (page {} of {})", 
	                 showDtos.size(), page, showsPage.getTotalPages());
	
	         return ResponseEntity.ok(ShowsListResponse.builder()
	                 .success(true)
	                 .message(ApiResponseMessage.FETCH_SUCCESS)
	                 .shows(showDtos)
	                 .page(showsPage.getNumber())
	                 .size(showsPage.getSize())
	                 .totalElements(showsPage.getTotalElements())
	                 .totalPages(showsPage.getTotalPages())
	                 .summary(summary)
	                 .build());
	
	     } catch (Exception e) {
	         log.error("Unexpected error retrieving all shows", e);
	         return buildShowsInternalErrorResponse(ApiResponseMessage.FETCH_FAILED);
	     }
	 }
	 
	// ==================== WEB SOCKET INTEGRATION METHODS ====================

	 /**
	  * Broadcasts show creation event to all POS systems.
	  * Called when a new show is successfully scheduled.
	  * 
	  * @param show The newly created show
	  */
	 @Override
	 public void broadcastShowCreated(Show show) {
	     try {
	         SystemEvent showCreatedEvent = SystemEvent.builder()
	                 .type(SystemEventType.INFO.getValue())
	                 .message(String.format("New show scheduled: %s at %s", 
	                         getMovieTitle(show.getMovieId()), 
	                         show.getStartAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))))
	                 .timestamp(LocalDateTime.now())
	                 .action(SystemEventAction.REFRESH_SHOWS.getValue())
	                 .build();
	         
	         webSocketService.broadcastSystemEvent(showCreatedEvent);
	         
	         log.info("Broadcasted show creation - Show: {}, Movie: {}, Time: {}", 
	                 show.getPublicId(), show.getMoviePublicId(), show.getStartAt());
	                 
	     } catch (Exception e) {
	         log.error("Failed to broadcast show creation for show: {}", show.getPublicId(), e);
	     }
	 }

	 /**
	  * Broadcasts show status update to all POS systems.
	  * Called when a show's status changes (SCHEDULED → RUNNING → COMPLETED, etc.)
	  * 
	  * @param showPublicId The show identifier
	  * @param oldStatus Previous status code
	  * @param newStatus New status code
	  */
	 @Override
	 public void broadcastShowStatusUpdate(String showPublicId, Integer oldStatus, Integer newStatus) {
	     try {
	         String oldStatusLabel = com.telecamnig.cinemapos.utility.Constants.ShowStatus.fromCode(oldStatus).getLabel();
	         String newStatusLabel = com.telecamnig.cinemapos.utility.Constants.ShowStatus.fromCode(newStatus).getLabel();
	         
	         SystemEvent statusUpdateEvent = SystemEvent.builder()
	                 .type(SystemEventType.INFO.getValue())
	                 .message(String.format("Show status updated: %s → %s", oldStatusLabel, newStatusLabel))
	                 .timestamp(LocalDateTime.now())
	                 .action(SystemEventAction.REFRESH_SHOW_STATUS.getValue())
	                 .build();
	         
	         // Broadcast to general system events
	         webSocketService.broadcastSystemEvent(statusUpdateEvent);
	         
	         // Also broadcast to show-specific topic for real-time updates
	         String showSpecificMessage = String.format("SHOW_STATUS_UPDATE:%s:%d:%d", 
	                 showPublicId, oldStatus, newStatus);
	         webSocketService.broadcastToShow(showPublicId, showSpecificMessage);
	         
	         log.info("Broadcasted show status update - Show: {}, {} → {}", 
	                 showPublicId, oldStatusLabel, newStatusLabel);
	                 
	     } catch (Exception e) {
	         log.error("Failed to broadcast show status update for show: {}", showPublicId, e);
	     }
	 }

	 /**
	  * Broadcasts show cancellation to all POS systems.
	  * Called when a show is cancelled, notifying all POS systems to update their views.
	  * 
	  * @param showPublicId The cancelled show identifier
	  * @param reason Reason for cancellation
	  */
	 @Override
	 public void broadcastShowCancelled(String showPublicId, String reason) {
	     try {
	         SystemEvent cancellationEvent = SystemEvent.builder()
	                 .type(SystemEventType.WARNING.getValue())
	                 .message(String.format("Show cancelled: %s. Reason: %s", showPublicId, reason))
	                 .timestamp(LocalDateTime.now())
	                 .action(SystemEventAction.REMOVE_SHOW.getValue())
	                 .build();
	         
	         webSocketService.broadcastSystemEvent(cancellationEvent);
	         
	         log.info("Broadcasted show cancellation - Show: {}, Reason: {}", showPublicId, reason);
	                 
	     } catch (Exception e) {
	         log.error("Failed to broadcast show cancellation for show: {}", showPublicId, e);
	     }
	 }

    // ==================== RESPONSE BUILDER METHODS FOR COMMON API RESPONSE ====================

	 private ResponseEntity<CommonApiResponse> buildCommonUnauthorizedResponse() {
	     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	             .body(new CommonApiResponse(false, ApiResponseMessage.UNAUTHORIZED_ACCESS));
	 }
	
	 private ResponseEntity<CommonApiResponse> buildCommonBadRequestResponse(String message) {
	     return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	             .body(new CommonApiResponse(false, message));
	 }
	
	 private ResponseEntity<CommonApiResponse> buildCommonNotFoundResponse(String message) {
	     return ResponseEntity.status(HttpStatus.NOT_FOUND)
	             .body(new CommonApiResponse(false, message));
	 }
	
	 private ResponseEntity<CommonApiResponse> buildCommonInternalErrorResponse(String message) {
	     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	             .body(new CommonApiResponse(false, message));
	 }
	
	 private ResponseEntity<ShowsListResponse> buildShowsNotFoundResponse(String message) {
	     return ResponseEntity.status(HttpStatus.NOT_FOUND)
	             .body(ShowsListResponse.builder()
	                     .success(false)
	                     .message(message)
	                     .build());
	 }

    // ==================== PRIVATE HELPER METHODS ====================

    private ShowDetailDto buildShowDetailDto(Show show, Movie movie, Screen screen, SeatMapSummaryDto seatSummary) {
        MovieDto movieDto = convertToMovieDto(movie);
        ScreenDto screenDto = convertToScreenDto(screen);
        
        // Get human-readable status label
        String statusLabel = ShowStatus.fromCode(show.getStatus()).getLabel();

        return ShowDetailDto.builder()
                .publicId(show.getPublicId())
                .movie(movieDto)
                .screen(screenDto)
                .layoutJson(screen.getLayoutJson())
                .startAt(show.getStartAt())
                .endAt(show.getEndAt())
                .status(show.getStatus())
                .statusLabel(statusLabel)
                .availableSeats(seatSummary.getAvailableSeats())
                .totalSeats(seatSummary.getTotalSeats())
                .bookedSeats(seatSummary.getSoldSeats() + seatSummary.getHeldSeats())
                .minPrice(seatSummary.getMinPrice())
                .maxPrice(seatSummary.getMaxPrice())
                .createdAt(show.getCreatedAt())
                .build();
    }

    private ShowDetailDto buildBasicShowDetailDto(Show show) {
        return ShowDetailDto.builder()
                .publicId(show.getPublicId())
                .startAt(show.getStartAt())
                .endAt(show.getEndAt())
                .status(show.getStatus())
                .statusLabel(ShowStatus.fromCode(show.getStatus()).getLabel())
                .createdAt(show.getCreatedAt())
                .build();
    }

    private SeatMapSummaryDto calculateSeatSummary(Long showId) {
        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);
        
        int totalSeats = showSeats.size();
        int availableSeats = 0;
        int heldSeats = 0;
        int soldSeats = 0;
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        Map<String, Integer> seatsByType = new HashMap<>();

        for (ShowSeat seat : showSeats) {
            // Count by state
            if (ShowSeatState.AVAILABLE.getLabel().equals(seat.getState())) {
                availableSeats++;
            } else if (ShowSeatState.HELD.getLabel().equals(seat.getState())) {
                heldSeats++;
            } else if (ShowSeatState.SOLD.getLabel().equals(seat.getState())) {
                soldSeats++;
            }

            // Calculate price range
            if (minPrice == null || seat.getPrice().compareTo(minPrice) < 0) {
                minPrice = seat.getPrice();
            }
            if (maxPrice == null || seat.getPrice().compareTo(maxPrice) > 0) {
                maxPrice = seat.getPrice();
            }

            // Count by seat type
            seatsByType.merge(seat.getSeatType(), 1, Integer::sum);
        }

        // Handle case where all seats might be unavailable
        if (minPrice == null) minPrice = BigDecimal.ZERO;
        if (maxPrice == null) maxPrice = BigDecimal.ZERO;

        return SeatMapSummaryDto.builder()
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .heldSeats(heldSeats)
                .soldSeats(soldSeats)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .seatsByType(seatsByType)
                .build();
    }

    private ShowSeatDto convertToShowSeatDto(ShowSeat showSeat) {
        try {
            // Parse layout metadata JSON
            Map<String, Object> layoutMeta = null;
            if (showSeat.getLayoutMetaJson() != null && !showSeat.getLayoutMetaJson().trim().isEmpty()) {
                layoutMeta = objectMapper.readValue(showSeat.getLayoutMetaJson(), 
                    new TypeReference<Map<String, Object>>() {});
            }

            // Get seat type display name
            String seatTypeDisplay = "";
            try {
                SeatType seatType = SeatType.fromValue(showSeat.getSeatType());
                seatTypeDisplay = seatType.getValue();
            } catch (IllegalArgumentException e) {
                log.debug("Invalid seat type in conversion: {}", showSeat.getSeatType());
                seatTypeDisplay = showSeat.getSeatType(); // Fallback to raw value
            }

            // Determine if seat is selectable
            boolean isSelectable = ShowSeatState.AVAILABLE.getLabel().equals(showSeat.getState());

            return ShowSeatDto.builder()
                    .publicId(showSeat.getPublicId())
                    .seatPublicId(showSeat.getSeatPublicId())
                    .seatLabel(showSeat.getSeatLabel())
                    .seatType(showSeat.getSeatType())
                    .seatTypeDisplay(seatTypeDisplay)
                    .state(showSeat.getState())
                    .price(showSeat.getPrice())
                    .rowIndex(showSeat.getRowIndex())
                    .colIndex(showSeat.getColIndex())
                    .groupPublicId(showSeat.getGroupPublicId())
                    .layoutMeta(layoutMeta)
                    .isSelectable(isSelectable)
                    .build();

        } catch (Exception e) {
            log.error("Error converting ShowSeat to DTO for seat: {}", showSeat.getPublicId(), e);
            // Return basic DTO without layout metadata
            return ShowSeatDto.builder()
                    .publicId(showSeat.getPublicId())
                    .seatPublicId(showSeat.getSeatPublicId())
                    .seatLabel(showSeat.getSeatLabel())
                    .seatType(showSeat.getSeatType())
                    .state(showSeat.getState())
                    .price(showSeat.getPrice())
                    .rowIndex(showSeat.getRowIndex())
                    .colIndex(showSeat.getColIndex())
                    .groupPublicId(showSeat.getGroupPublicId())
                    .isSelectable(ShowSeatState.AVAILABLE.getLabel().equals(showSeat.getState()))
                    .build();
        }
    }

    private MovieDto convertToMovieDto(Movie movie) {
        return MovieDto.builder()
                .publicId(movie.getPublicId())
                .title(movie.getTitle())
                .durationMinutes(movie.getDurationMinutes())
                .genres(movie.getGenres())
                .language(movie.getLanguage())
                .posterPath(movie.getPosterPath())
                .rating(movie.getRating())
                .parentalRating(movie.getParentalRating())
                .is3D(movie.getIs3D())
                .isIMAX(movie.getIsIMAX())
                .releaseDate(movie.getReleaseDate())
                .status(movie.getStatus())
                .build();
    }

    private ScreenDto convertToScreenDto(Screen screen) {
        return ScreenDto.builder()
                .publicId(screen.getPublicId())
                .code(screen.getCode())
                .name(screen.getName())
                .category(screen.getCategory())
                .layoutJson(screen.getLayoutJson())
                .totalSeats(extractTotalSeatsFromLayout(screen.getLayoutJson()))
                .capacity(extractTotalSeatsFromLayout(screen.getLayoutJson()))
                .build();
    }

    private Integer extractTotalSeatsFromLayout(String layoutJson) {
        try {
            if (layoutJson != null && !layoutJson.trim().isEmpty()) {
                Map<String, Object> layout = objectMapper.readValue(layoutJson, 
                    new TypeReference<Map<String, Object>>() {});
                if (layout.containsKey("metadata")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = (Map<String, Object>) layout.get("metadata");
                    if (metadata.containsKey("totalSeats")) {
                        return (Integer) metadata.get("totalSeats");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting total seats from layout JSON", e);
        }
        return 0; // Default fallback
    }
    
   // ==================== PRIVATE HELPER METHODS FOR PHASE 2 ====================

    private ShowListItemDto convertToShowListItemDto(Show show) {
        try {
            // Fetch related entities efficiently
            Movie movie = movieRepository.findById(show.getMovieId()).orElse(null);
            Screen screen = screenRepository.findById(show.getScreenId()).orElse(null);
            
            // Calculate seat availability
            SeatMapSummaryDto seatSummary = calculateSeatSummary(show.getId());
            
            // Get human-readable status label
            String statusLabel = com.telecamnig.cinemapos.utility.Constants.ShowStatus.fromCode(show.getStatus()).getLabel();

            return ShowListItemDto.builder()
                    .publicId(show.getPublicId())
                    .moviePublicId(show.getMoviePublicId())
                    .movieTitle(movie != null ? movie.getTitle() : "Unknown Movie")
                    .posterPath(movie != null ? movie.getPosterPath() : null)
                    .language(movie != null ? movie.getAudioLanguages() : "")
                    .durationMinutes(movie != null ? movie.getDurationMinutes() : 0)
                    .screenPublicId(show.getScreenPublicId())
                    .screenName(screen != null ? screen.getName() : "Unknown Screen")
                    .screenCategory(screen != null ? screen.getCategory() : "UNKNOWN")
                    .startAt(show.getStartAt())
                    .endAt(show.getEndAt())
                    .status(show.getStatus())
                    .statusLabel(statusLabel)
                    .availableSeats(seatSummary.getAvailableSeats())
                    .totalSeats(seatSummary.getTotalSeats())
                    .minPrice(seatSummary.getMinPrice())
                    .maxPrice(seatSummary.getMaxPrice())
                    .createdAt(show.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error converting Show to ShowListItemDto for show: {}", show.getPublicId(), e);
            // Return basic DTO without related entity data
            return ShowListItemDto.builder()
                    .publicId(show.getPublicId())
                    .moviePublicId(show.getMoviePublicId())
                    .movieTitle("Unknown Movie")
                    .screenPublicId(show.getScreenPublicId())
                    .screenName("Unknown Screen")
                    .startAt(show.getStartAt())
                    .endAt(show.getEndAt())
                    .status(show.getStatus())
                    .statusLabel(com.telecamnig.cinemapos.utility.Constants.ShowStatus.fromCode(show.getStatus()).getLabel())
                    .createdAt(show.getCreatedAt())
                    .build();
        }
    }

    private ShowsSummaryDto buildUpcomingShowsSummary(List<Show> shows) {
        Map<String, Integer> showsByScreen = new HashMap<>();
        Map<String, Integer> showsByMovie = new HashMap<>();
        
        for (Show show : shows) {
            Screen screen = screenRepository.findById(show.getScreenId()).orElse(null);
            Movie movie = movieRepository.findById(show.getMovieId()).orElse(null);
            
            String screenName = screen != null ? screen.getCode() : "Unknown";
            String movieTitle = movie != null ? movie.getTitle() : "Unknown";
            
            showsByScreen.merge(screenName, 1, Integer::sum);
            showsByMovie.merge(movieTitle, 1, Integer::sum);
        }
        
        return ShowsSummaryDto.builder()
                .totalShows(shows.size())
                .upcomingShows(shows.size())
                .runningShows(0)
                .completedShows(0)
                .cancelledShows(0)
                .totalAvailableSeats(calculateTotalAvailableSeats(shows))
                .showsByScreen(showsByScreen)
                .showsByMovie(showsByMovie)
                .build();
    }

    private ShowsSummaryDto buildRunningShowsSummary(List<Show> shows) {
        Map<String, Integer> showsByScreen = new HashMap<>();
        Map<String, Integer> showsByMovie = new HashMap<>();
        
        for (Show show : shows) {
            Screen screen = screenRepository.findById(show.getScreenId()).orElse(null);
            Movie movie = movieRepository.findById(show.getMovieId()).orElse(null);
            
            String screenName = screen != null ? screen.getCode() : "Unknown";
            String movieTitle = movie != null ? movie.getTitle() : "Unknown";
            
            showsByScreen.merge(screenName, 1, Integer::sum);
            showsByMovie.merge(movieTitle, 1, Integer::sum);
        }
        
        return ShowsSummaryDto.builder()
                .totalShows(shows.size())
                .upcomingShows(0)
                .runningShows(shows.size())
                .completedShows(0)
                .cancelledShows(0)
                .totalAvailableSeats(calculateTotalAvailableSeats(shows))
                .showsByScreen(showsByScreen)
                .showsByMovie(showsByMovie)
                .build();
    }

    private ShowsSummaryDto buildActiveShowsSummary(List<Show> shows) {
        Map<String, Integer> showsByScreen = new HashMap<>();
        Map<String, Integer> showsByMovie = new HashMap<>();
        int upcomingCount = 0;
        int runningCount = 0;
        
        for (Show show : shows) {
            Screen screen = screenRepository.findById(show.getScreenId()).orElse(null);
            Movie movie = movieRepository.findById(show.getMovieId()).orElse(null);
            
            String screenName = screen != null ? screen.getCode() : "Unknown";
            String movieTitle = movie != null ? movie.getTitle() : "Unknown";
            
            showsByScreen.merge(screenName, 1, Integer::sum);
            showsByMovie.merge(movieTitle, 1, Integer::sum);
            
            // Count by status
            if (show.getStatus() == com.telecamnig.cinemapos.utility.Constants.ShowStatus.SCHEDULED.getCode()) {
                upcomingCount++;
            } else if (show.getStatus() == com.telecamnig.cinemapos.utility.Constants.ShowStatus.RUNNING.getCode()) {
                runningCount++;
            }
        }
        
        return ShowsSummaryDto.builder()
                .totalShows(shows.size())
                .upcomingShows(upcomingCount)
                .runningShows(runningCount)
                .completedShows(0)
                .cancelledShows(0)
                .totalAvailableSeats(calculateTotalAvailableSeats(shows))
                .showsByScreen(showsByScreen)
                .showsByMovie(showsByMovie)
                .build();
    }

    private ShowsSummaryDto buildHistoryShowsSummary(List<Show> shows) {
        Map<String, Integer> showsByScreen = new HashMap<>();
        Map<String, Integer> showsByMovie = new HashMap<>();
        int completedCount = 0;
        int cancelledCount = 0;
        
        for (Show show : shows) {
            Screen screen = screenRepository.findById(show.getScreenId()).orElse(null);
            Movie movie = movieRepository.findById(show.getMovieId()).orElse(null);
            
            String screenName = screen != null ? screen.getCode() : "Unknown";
            String movieTitle = movie != null ? movie.getTitle() : "Unknown";
            
            showsByScreen.merge(screenName, 1, Integer::sum);
            showsByMovie.merge(movieTitle, 1, Integer::sum);
            
            // Count by status
            if (show.getStatus() == com.telecamnig.cinemapos.utility.Constants.ShowStatus.COMPLETED.getCode()) {
                completedCount++;
            } else if (show.getStatus() == com.telecamnig.cinemapos.utility.Constants.ShowStatus.CANCELLED.getCode()) {
                cancelledCount++;
            }
        }
        
        return ShowsSummaryDto.builder()
                .totalShows(shows.size())
                .upcomingShows(0)
                .runningShows(0)
                .completedShows(completedCount)
                .cancelledShows(cancelledCount)
                .totalAvailableSeats(0) // Historical shows don't have available seats
                .showsByScreen(showsByScreen)
                .showsByMovie(showsByMovie)
                .build();
    }

    private Integer calculateTotalAvailableSeats(List<Show> shows) {
        int totalAvailable = 0;
        for (Show show : shows) {
            SeatMapSummaryDto seatSummary = calculateSeatSummary(show.getId());
            totalAvailable += seatSummary.getAvailableSeats();
        }
        return totalAvailable;
    }
    
 // ==================== PRIVATE HELPER METHODS FOR PHASE 3 ====================
	
 	 private Page<Show> executeAdvancedSearch(ShowSearchRequest request, Pageable pageable) {
 	     // This is a simplified implementation - you might want to use Specification API for more complex searches
 	     
 	     if (request.getMoviePublicId() != null) {
 	         Movie movie = movieRepository.findByPublicId(request.getMoviePublicId())
 	                 .orElseThrow(() -> new IllegalArgumentException(ApiResponseMessage.MOVIE_NOT_FOUND));
 	         return showRepository.findByMovieId(movie.getId(), pageable);
 	     }
 	     
 	     if (request.getScreenPublicId() != null) {
 	         Screen screen = screenRepository.findByPublicId(request.getScreenPublicId())
 	                 .orElseThrow(() -> new IllegalArgumentException(ApiResponseMessage.SCREEN_NOT_FOUND));
 	         return showRepository.findByScreenId(screen.getId(), pageable);
 	     }
 	     
 	     if (request.getDate() != null) {
 	         LocalDateTime startOfDay = request.getDate().atStartOfDay();
 	         LocalDateTime endOfDay = request.getDate().plusDays(1).atStartOfDay();
 	         return showRepository.findByStartAtBetween(startOfDay, endOfDay, pageable);
 	     }
 	     
 	     if (request.getStatus() != null) {
 	         return showRepository.findByStatus(request.getStatus(), pageable);
 	     }
 	     
 	     // Default: return all shows with pagination
 	     return showRepository.findAll(pageable);
 	 }
 	
 	 private boolean isValidStatusTransition(Integer currentStatus, Integer newStatus) {
 	     // Define valid status transitions
 	     // SCHEDULED -> RUNNING, CANCELLED
 	     // RUNNING -> COMPLETED
 	     // COMPLETED -> (no transitions)
 	     // CANCELLED -> (no transitions)
 	     
 	     if (currentStatus.equals(newStatus)) {
 	         return true; // No change is valid
 	     }
 	     
 	     switch (currentStatus) {
 	         case 0: // SCHEDULED
 	             return newStatus == 1 || newStatus == 3; // Can become RUNNING or CANCELLED
 	         case 1: // RUNNING
 	             return newStatus == 2; // Can only become COMPLETED
 	         case 2: // COMPLETED
 	             return false; // Cannot change from COMPLETED
 	         case 3: // CANCELLED
 	             return false; // Cannot change from CANCELLED
 	         default:
 	             return false;
 	     }
 	 }
 	
 	 private boolean canShowBeCancelled(Show show) {
 	     // Only SCHEDULED shows can be cancelled
 	     return show.getStatus() == com.telecamnig.cinemapos.utility.Constants.ShowStatus.SCHEDULED.getCode();
 	 }
 	
// 	 private void releaseHeldSeatsForShow(Long showId) {
// 	     try {
// 	         List<ShowSeat> heldSeats = showSeatRepository.findByShowIdAndState(showId, 
// 	             com.telecamnig.cinemapos.utility.Constants.ShowSeatState.HELD.getLabel());
// 	         
// 	         for (ShowSeat seat : heldSeats) {
// 	             seat.setState(com.telecamnig.cinemapos.utility.Constants.ShowSeatState.AVAILABLE.getLabel());
// 	             seat.setReservedBy(null);
// 	             seat.setReservedAt(null);
// 	             seat.setExpiresAt(null);
// 	         }
// 	         
// 	         showSeatRepository.saveAll(heldSeats);
// 	         log.info("Released {} held seats for cancelled show: {}", heldSeats.size(), showId);
// 	     } catch (Exception e) {
// 	         log.error("Error releasing held seats for show: {}", showId, e);
// 	     }
// 	 }

 	 // Summary builder methods for Phase 3
 	 private ShowsSummaryDto buildSearchSummary(List<Show> shows, ShowSearchRequest request) {
 	     // Implementation similar to previous summary methods
 	     return buildGenericShowsSummary(shows, "Search Results");
 	 }
 	
 	 private ShowsSummaryDto buildMovieShowsSummary(List<Show> shows, Movie movie) {
 	     ShowsSummaryDto summary = buildGenericShowsSummary(shows, "Movie: " + movie.getTitle());
 	     // Add movie-specific summary data if needed
 	     return summary;
 	 }
 	
 	 private ShowsSummaryDto buildScreenShowsSummary(List<Show> shows, Screen screen) {
 	     ShowsSummaryDto summary = buildGenericShowsSummary(shows, "Screen: " + screen.getName());
 	     // Add screen-specific summary data if needed
 	     return summary;
 	 }
 	
 	 private ShowsSummaryDto buildDateShowsSummary(List<Show> shows, LocalDate date) {
 	     ShowsSummaryDto summary = buildGenericShowsSummary(shows, "Date: " + date.toString());
 	     // Add date-specific summary data if needed
 	     return summary;
 	 }
 	
 	 private ShowsSummaryDto buildAllShowsSummary(List<Show> shows) {
 	     return buildGenericShowsSummary(shows, "All Shows");
 	 }
 	
 	 private ShowsSummaryDto buildGenericShowsSummary(List<Show> shows, String description) {
 	     Map<String, Integer> showsByScreen = new HashMap<>();
 	     Map<String, Integer> showsByMovie = new HashMap<>();
 	     Map<Integer, Integer> showsByStatus = new HashMap<>();
 	     
 	     for (Show show : shows) {
 	         Screen screen = screenRepository.findById(show.getScreenId()).orElse(null);
 	         Movie movie = movieRepository.findById(show.getMovieId()).orElse(null);
 	         
 	         String screenName = screen != null ? screen.getCode() : "Unknown";
 	         String movieTitle = movie != null ? movie.getTitle() : "Unknown";
 	         
 	         showsByScreen.merge(screenName, 1, Integer::sum);
 	         showsByMovie.merge(movieTitle, 1, Integer::sum);
 	         showsByStatus.merge(show.getStatus(), 1, Integer::sum);
 	     }
 	     
 	     return ShowsSummaryDto.builder()
 	             .totalShows(shows.size())
 	             .upcomingShows(showsByStatus.getOrDefault(0, 0))
 	             .runningShows(showsByStatus.getOrDefault(1, 0))
 	             .completedShows(showsByStatus.getOrDefault(2, 0))
 	             .cancelledShows(showsByStatus.getOrDefault(3, 0))
 	             .totalAvailableSeats(calculateTotalAvailableSeats(shows))
 	             .showsByScreen(showsByScreen)
 	             .showsByMovie(showsByMovie)
 	             .build();
 	 }

    // ==================== RESPONSE BUILDER METHODS ====================

    private ResponseEntity<ShowWithSeatsResponse> buildUnauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ShowWithSeatsResponse.builder()
                        .success(false)
                        .message(ApiResponseMessage.UNAUTHORIZED_ACCESS)
                        .build());
    }

    private ResponseEntity<ShowWithSeatsResponse> buildBadRequestResponse(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ShowWithSeatsResponse.builder()
                        .success(false)
                        .message(message)
                        .build());
    }

    private ResponseEntity<ShowWithSeatsResponse> buildNotFoundResponse(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ShowWithSeatsResponse.builder()
                        .success(false)
                        .message(message)
                        .build());
    }

    private ResponseEntity<ShowWithSeatsResponse> buildInternalErrorResponse(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ShowWithSeatsResponse.builder()
                        .success(false)
                        .message(message)
                        .build());
    }
    
 // ==================== RESPONSE BUILDER METHODS FOR PHASE 2 ====================

    private ResponseEntity<ShowsListResponse> buildShowsUnauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ShowsListResponse.builder()
                        .success(false)
                        .message(ApiResponseMessage.UNAUTHORIZED_ACCESS)
                        .build());
    }

    private ResponseEntity<ShowsListResponse> buildShowsBadRequestResponse(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ShowsListResponse.builder()
                        .success(false)
                        .message(message)
                        .build());
    }

    private ResponseEntity<ShowsListResponse> buildShowsInternalErrorResponse(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ShowsListResponse.builder()
                        .success(false)
                        .message(message)
                        .build());
    }

    // ==================== SECURITY & AUDIT HELPERS ====================

    private Authentication getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        return auth;
    }

    private Long extractCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return null;
            Object principal = auth.getPrincipal();
            if (principal == null) return null;

            // if principal has getId(), use it
            try {
                var getId = principal.getClass().getMethod("getId");
                Object idObj = getId.invoke(principal);
                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();
                }
            } catch (NoSuchMethodException ignored) {}

            // fallback parse name if numeric
            String name = auth.getName();
            if (name != null && name.matches("\\d+")) {
                return Long.parseLong(name);
            }
        } catch (Exception ex) {
            log.debug("Failed to extract user id from principal: {}", ex.toString());
        }
        return null;
    }
    
    private void validateSeatTypes(Map<String, BigDecimal> seatPrices) {
        for (String seatTypeName : seatPrices.keySet()) {
            if (!SeatType.isValidValue(seatTypeName)) {
                throw new InvalidSeatTypeException("Invalid seat type: " + seatTypeName);
            }
            
            // Validate price is positive
            BigDecimal price = seatPrices.get(seatTypeName);
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidSeatTypeException("Price must be positive for seat type: " + seatTypeName);
            }
        }
    }

    private LocalDateTime calculateEndTime(LocalDateTime startAt, Integer movieDuration) {
        if (movieDuration == null || movieDuration <= 0) {
            throw new IllegalArgumentException("Movie duration must be positive");
        }
        return startAt.plusMinutes(movieDuration).plusMinutes(SHOW_BUFFER_MINUTES);
    }

    private void checkTimeConflicts(Long screenId, LocalDateTime startAt, LocalDateTime endAt) {
        // Check for overlapping shows on the same screen using repository method
        // Only check SCHEDULED and RUNNING shows (not COMPLETED or CANCELLED)
        List<Show> conflictingShows = showRepository.findConflictingShows(
                screenId, 
                startAt, 
                endAt);
        
        if (!conflictingShows.isEmpty()) {
            Show conflictShow = conflictingShows.get(0);
            String conflictMessage = String.format(
                    "Time conflict with existing show '%s' from %s to %s. Screen is occupied during this time.",
                    conflictShow.getMoviePublicId(), 
                    conflictShow.getStartAt(), 
                    conflictShow.getEndAt());
            throw new ShowConflictException(conflictMessage);
        }
    }

    private Show createShowEntity(ScheduleShowRequest request, Movie movie, Screen screen, LocalDateTime endAt) {
        Show.ShowBuilder showBuilder = Show.builder()
                .movieId(movie.getId())
                .screenId(screen.getId())
                .moviePublicId(movie.getPublicId())
                .screenPublicId(screen.getPublicId())
                .layoutJson(screen.getLayoutJson())
                .startAt(request.getStartAt())
                .endAt(endAt);

        // Set status - use manual if provided, otherwise default to SCHEDULED
        if (request.getStatus() != null) {
            // Validate the provided status using ShowStatus enum
            if (!isValidShowStatus(request.getStatus())) {
                throw new IllegalArgumentException("Invalid show status: " + request.getStatus());
            }
            showBuilder.status(request.getStatus());
        } else {
            showBuilder.status(ShowStatus.SCHEDULED.getCode());
        }

        // Set createdBy if available
        Long currentUserId = extractCurrentUserId();
        if (currentUserId != null) {
            showBuilder.createdBy(currentUserId);
        }

        return showBuilder.build();
    }

    private boolean isValidShowStatus(Integer status) {
        return ShowStatus.fromCode(status) != null;
    }

    private void generateShowSeats(Show show, Long screenId, Map<String, BigDecimal> seatPrices) {
        // Get all active seats for the screen using SeatStatus enum
        List<ScreenSeat> screenSeats = screenSeatRepository.findByScreenIdAndStatus(
                screenId, 
                SeatStatus.ACTIVE.getCode());
        
        if (screenSeats.isEmpty()) {
            log.warn("No active seats found for screen ID: {}", screenId);
            throw new IllegalArgumentException("Screen has no active seats configured");
        }

        List<ShowSeat> showSeats = new ArrayList<>();

        for (ScreenSeat screenSeat : screenSeats) {
            // Convert stored seat type to enum and get display value for price lookup
            SeatType seatType;
            try {
                seatType = SeatType.fromValue(screenSeat.getSeatType());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid seat type '{}' for seat {} in screen {}, skipping", 
                        screenSeat.getSeatType(), screenSeat.getLabel(), screenId);
                continue; // Skip invalid seat types
            }
            
            String seatTypeDisplayValue = seatType.getValue(); // "Regular", "Gold", etc.
            
            BigDecimal price = seatPrices.get(seatTypeDisplayValue);
            if (price == null) {
                throw new InvalidSeatTypeException("No price provided for seat type: " + seatTypeDisplayValue);
            }

            ShowSeat showSeat = ShowSeat.builder()
                    .showId(show.getId())
                    .seatPublicId(screenSeat.getPublicId())
                    .seatLabel(screenSeat.getLabel())
                    .seatType(seatType.name()) // Store enum name "REGULAR", "GOLD" in DB
                    .layoutMetaJson(screenSeat.getMetaJson()) // Historical snapshot
                    .rowIndex(screenSeat.getRowIndex())
                    .colIndex(screenSeat.getColIndex())
                    .groupPublicId(screenSeat.getGroupPublicId())
                    .price(price)
                    .state(ShowSeatState.AVAILABLE.getLabel()) // Use enum constant
                    .build();

            // Set createdBy if available
            Long currentUserId = extractCurrentUserId();
            if (currentUserId != null) {
                showSeat.setCreatedBy(currentUserId);
            }

            showSeats.add(showSeat);
        }

        if (showSeats.isEmpty()) {
            throw new IllegalArgumentException("No valid seats could be created for the show");
        }

        showSeatRepository.saveAll(showSeats);
        log.info("Generated {} show seats for show: {}", showSeats.size(), show.getPublicId());
    }

    private ShowDto mapToShowDto(Show show) {
        return ShowDto.builder()
                .publicId(show.getPublicId())
                .moviePublicId(show.getMoviePublicId())
                .screenPublicId(show.getScreenPublicId())
                .startAt(show.getStartAt())
                .endAt(show.getEndAt())
                .status(show.getStatus())
                .createdAt(show.getCreatedAt())
                .build();
    }
    
    /**
     * Helper method to get movie title for broadcasting
     */
    private String getMovieTitle(Long movieId) {
        try {
            Optional<Movie> movie = movieRepository.findById(movieId);
            return movie.map(Movie::getTitle).orElse("Unknown Movie");
        } catch (Exception e) {
            log.error("Error getting movie title for movieId: {}", movieId, e);
            return "Unknown Movie";
        }
    }

    /**
     * Helper method to release held seats for a cancelled show
     */
    private void releaseHeldSeatsForShow(Long showId) {
        try {
            // Find all held seats for this show
            List<ShowSeat> heldSeats = showSeatRepository.findByShowIdAndState(showId, ShowSeatState.HELD.getLabel());
            
            // Release each held seat
            for (ShowSeat seat : heldSeats) {
                seat.setState(ShowSeatState.AVAILABLE.getLabel());
                seat.setReservedBy(null);
                seat.setReservedAt(null);
                seat.setExpiresAt(null);
                
                // Broadcast seat release via WebSocket
                SeatStateEvent releaseEvent = SeatStateEvent.builder()
                    .showPublicId(findShowPublicId(showId))
                    .seatPublicId(seat.getSeatPublicId())
                    .state(ShowSeatState.AVAILABLE.getLabel())
                    .reservedBy(null)
                    .timestamp(LocalDateTime.now())
                    .eventType("SEAT_RELEASED")
                    .build();
                
                webSocketService.broadcastSeatUpdate(findShowPublicId(showId), releaseEvent);
            }
            
            showSeatRepository.saveAll(heldSeats);
            log.info("Released {} held seats for cancelled show: {}", heldSeats.size(), showId);
            
        } catch (Exception e) {
            log.error("Error releasing held seats for show: {}", showId, e);
        }
    }

    /**
     * Helper method to find show public ID by show ID
     */
    private String findShowPublicId(Long showId) {
        try {
            Optional<Show> show = showRepository.findById(showId);
            return show.map(Show::getPublicId).orElse(null);
        } catch (Exception e) {
            log.error("Error finding show public ID for showId: {}", showId, e);
            return null;
        }
    }

}