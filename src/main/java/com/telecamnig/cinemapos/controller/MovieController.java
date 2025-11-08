package com.telecamnig.cinemapos.controller;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.telecamnig.cinemapos.dto.AddMovieRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.MoviesResponse;
import com.telecamnig.cinemapos.dto.UpdateMovieRequest;
import com.telecamnig.cinemapos.dto.UpdateMovieStatusRequest;
import com.telecamnig.cinemapos.service.MovieService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/movie")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Add a new movie with poster image (Admin only).
     * POST /api/v1/movie/add
     * Accepts multipart form data with movie data and poster file.
     */
//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonApiResponse> addMovie(
            @Valid @RequestPart("movieData") AddMovieRequest request,
            @RequestPart(value = "poster", required = false) MultipartFile posterFile) {
        return movieService.addMovie(request, posterFile);
    }
    
    /**
     * Update movie status (any authenticated user).
     * PUT /api/v1/movie/{publicId}/status
     */
//    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{publicId}/status")
    public ResponseEntity<CommonApiResponse> updateMovieStatus(
            @PathVariable("publicId") String publicId,
            @Valid @RequestBody UpdateMovieStatusRequest request) {
        return movieService.updateMovieStatus(publicId, request.getStatus());
    }
    
    /**
     * Update movie details (excluding poster).
     * PUT /api/v1/movie/{publicId}/update
     * Any authenticated user can call this endpoint.
     */
//  @PreAuthorize("isAuthenticated()")
    @PutMapping("/{publicId}/update")
    public ResponseEntity<CommonApiResponse> updateMovieDetails(
            @PathVariable("publicId") String publicId,
            @Valid @RequestBody UpdateMovieRequest request) {
        return movieService.updateMovieDetails(publicId, request);
    }

    /**
     * Upload / update movie poster.
     * POST /api/v1/movie/{publicId}/poster
     * Any authenticated user can call this endpoint.
     * Note: This endpoint stores the file via LocalStorageService and updates movie.posterPath.
     */
//    @PreAuthorize("isAuthenticated()")
    @PostMapping(path = "/{publicId}/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonApiResponse> uploadMoviePoster(
            @PathVariable("publicId") String publicId,
            @RequestPart("poster") MultipartFile poster) {
        return movieService.updateMoviePoster(publicId, poster);
    }
    
//    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{publicId}")
    public ResponseEntity<MoviesResponse> getMovieByPublicId(@PathVariable("publicId") String publicId) {
        return movieService.getMovieByPublicId(publicId);
    }

//    @PreAuthorize("isAuthenticated()")
//    @GetMapping("/status/{status}")
//    public ResponseEntity<MoviesResponse> getMoviesByStatus(@PathVariable("status") int status) {
//        return movieService.getMoviesByStatus(status);
//    }
//    
//    @PreAuthorize("isAuthenticated()")
//    @GetMapping("/status/{status}/paged")
//    public ResponseEntity<MoviesResponse> getMoviesByStatusPaged(
//            @PathVariable("status") int status,
//            @PageableDefault(page = 0, size = 20)
//            @SortDefault.SortDefaults({
//                @SortDefault(sort = "releaseDate", direction = Sort.Direction.DESC)
//            })
//            Pageable pageable) {
//        return movieService.getMoviesByStatusPaged(status, pageable);
//    }
    
    /**
     * Fetch movies based on their status (e.g., Now Showing, Upcoming, etc.).
     *
     * This endpoint supports both:
     * 1. Non-paged mode (default): returns all movies for the given status.
     * 2. Paged mode: when `paged=true` is passed, applies pagination using Pageable.
     *
     * Example Usage:
     * - /api/movies/status/1 → Returns ALL movies (non-paged)
     * - /api/movies/status/1?paged=true&page=0&size=10 → Returns paginated movies
     *
     * @param status the movie status code (e.g., 1 = Now Showing, 2 = Upcoming, etc.)
     * @param pageable pagination and sorting parameters (only used if paged=true)
     * @param paged whether to enable pagination (default = false)
     * @return MoviesResponse containing list of movies (paged or full)
     */
//    @PreAuthorize("isAuthenticated()")
    @GetMapping("/status/{status}")
    public ResponseEntity<MoviesResponse> getMoviesByStatus(
            @PathVariable("status") int status,
            @PageableDefault(page = 0, size = 20)
            @SortDefault.SortDefaults({
                @SortDefault(sort = "releaseDate", direction = Sort.Direction.DESC)
            })
            Pageable pageable,
            @RequestParam(value = "paged", required = false, defaultValue = "false") boolean paged) {

        return movieService.getMoviesByStatus(status, pageable, paged);
    }

    
    /**
     * Serve poster file for a movie (authenticated).
     */
//    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{publicId}/poster")
    public ResponseEntity<Resource> getMoviePoster(@PathVariable("publicId") String publicId) {
        return movieService.getMoviePoster(publicId);
    }

    /**
     * Paginated fetch. Optional query params: status (int) and search (title).
     * GET /api/v1/movie?page=0&size=20&sort=releaseDate,desc&status=1&search=avengers
     */
//    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search-paginated")
    public ResponseEntity<MoviesResponse> getMovies(
            @PageableDefault(page = 0, size = 20)
            @SortDefault.SortDefaults({
                @SortDefault(sort = "releaseDate", direction = Sort.Direction.DESC)
            })
            Pageable pageable,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "search", required = false) String search) {
        return movieService.getMovies(pageable, status, search);
    }

    /**
     * Non-paginated fetch (all results). Optional filters status & search.
     * GET /api/v1/movie/all?status=1&search=avengers
     */
//    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    public ResponseEntity<MoviesResponse> getMoviesNoPage(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "search", required = false) String search) {
        return movieService.getMoviesNoPage(status, search);
    }
    
}
