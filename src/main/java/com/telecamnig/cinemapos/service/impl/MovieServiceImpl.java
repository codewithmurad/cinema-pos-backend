package com.telecamnig.cinemapos.service.impl;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.telecamnig.cinemapos.dto.AddMovieRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.MovieDto;
import com.telecamnig.cinemapos.dto.MoviesResponse;
import com.telecamnig.cinemapos.dto.UpdateMovieRequest;
import com.telecamnig.cinemapos.entity.Movie;
import com.telecamnig.cinemapos.repository.MovieRepository;
import com.telecamnig.cinemapos.service.MovieService;
import com.telecamnig.cinemapos.storage.LocalStorageService;
import com.telecamnig.cinemapos.utility.ApiResponseMessage;
import com.telecamnig.cinemapos.utility.Constants.MovieStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class MovieServiceImpl implements MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieServiceImpl.class);

    // derive allowed status codes from MovieStatus enum (single source of truth)
    private static final Set<Integer> ALLOWED_STATUS_CODES = Arrays.stream(MovieStatus.values())
            .map(MovieStatus::getCode)
            .collect(Collectors.toSet());

    private final MovieRepository movieRepository;
    
    private final Validator validator;

    private final LocalStorageService localStorageService;

    public MovieServiceImpl(MovieRepository movieRepository, Validator validator, LocalStorageService localStorageService) {
        this.movieRepository = movieRepository;
        this.validator = validator;
        this.localStorageService = localStorageService;
    }

    @Override
    @Transactional
    public ResponseEntity<CommonApiResponse> addMovie(AddMovieRequest request, MultipartFile posterFile) {
        
        // 🔒 Step 1: Check authentication
//        Authentication auth = getAuthenticatedUser();
//        if (auth == null) {
//            log.warn("Unauthorized access attempt to addMovie()");
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(new CommonApiResponse(false, ApiResponseMessage.UNAUTHORIZED_ACCESS));
//        }

        // Defensive validation
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<AddMovieRequest> v : violations) {
                sb.append(v.getPropertyPath()).append("=").append(v.getMessage()).append("; ");
            }
            log.warn("AddMovie request validation failed: {}", sb.toString());
            throw new ConstraintViolationException(violations);
        }

        // status validation using MovieStatus enum codes
        if (request.getStatus() == null || !ALLOWED_STATUS_CODES.contains(request.getStatus())) {
            log.warn("Invalid status for AddMovie: {}", request.getStatus());
            return ResponseEntity.badRequest()
                    .body(new CommonApiResponse(false, ApiResponseMessage.INVALID_MOVIE_STATUS_CODE));
        }

        // Title uniqueness
        String title = request.getTitle().trim();
        if (movieRepository.existsByTitleIgnoreCase(title)) {
            log.warn("Duplicate movie title attempted: {}", title);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CommonApiResponse(false, ApiResponseMessage.MOVIE_ALREADY_EXISTS));
        }

        // ✅ Handle poster file upload using your LocalStorageService
        String posterPath = null;
        if (posterFile != null && !posterFile.isEmpty()) {
            try {
                posterPath = localStorageService.store(posterFile);
                log.info("Poster uploaded successfully via LocalStorageService: {}", posterPath);
            } catch (IOException e) {
                log.error("Failed to upload poster image via LocalStorageService", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new CommonApiResponse(false, "Failed to upload poster image: " + e.getMessage()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid poster file: {}", e.getMessage());
                return ResponseEntity.badRequest()
                        .body(new CommonApiResponse(false, e.getMessage()));
            } catch (SecurityException e) {
                log.error("Security violation in file upload: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new CommonApiResponse(false, "Security violation: " + e.getMessage()));
            }
        }

        // Map DTO -> Entity
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDescription(trimToNull(request.getDescription()));
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setGenres(trimToNull(request.getGenres()));
        movie.setLanguage(trimToNull(request.getLanguage()));
        movie.setCountry(trimToNull(request.getCountry()));
        movie.setDirector(trimToNull(request.getDirector()));
        movie.setCastMembers(trimToNull(request.getCastMembers()));
        movie.setProducers(trimToNull(request.getProducers()));
        movie.setProductionCompany(trimToNull(request.getProductionCompany()));
        movie.setDistributor(trimToNull(request.getDistributor()));
        movie.setPosterPath(posterPath); // ✅ Set the uploaded poster path from storage service
        movie.setIs3D(request.getIs3D() != null ? request.getIs3D() : Boolean.FALSE);
        movie.setIsIMAX(request.getIsIMAX() != null ? request.getIsIMAX() : Boolean.FALSE);
        movie.setSubtitlesAvailable(request.getSubtitlesAvailable() != null ? request.getSubtitlesAvailable() : Boolean.FALSE);
        movie.setAudioLanguages(trimToNull(request.getAudioLanguages()));
        movie.setRating(request.getRating() != null ? request.getRating() : 0.0);
        movie.setParentalRating(trimToNull(request.getParentalRating()));
        movie.setReleaseDate(request.getReleaseDate());
        movie.setStatus(request.getStatus());

        // audit: set createdBy if resolvable
        Long currentUserId = extractCurrentUserId();
        if (currentUserId != null) {
            movie.setCreatedBy(currentUserId);
        }
        // createdAt/updatedAt handled by entity lifecycle hooks

        Movie saved = movieRepository.save(movie);
        log.info("Movie saved: id={}, publicId={}, title={}", saved.getId(), saved.getPublicId(), saved.getTitle());

        // Return created response with publicId included in message (CommonApiResponse has no data field)
        String msg = ApiResponseMessage.MOVIE_CREATED + " publicId:" + saved.getPublicId();
        return ResponseEntity.status(HttpStatus.CREATED).body(new CommonApiResponse(true, msg));
    }
    
    @Override
    @Transactional
    public ResponseEntity<CommonApiResponse> updateMovieStatus(String publicId, Integer status) {
    	
    	// 🔒 Step 1: Check authentication
        Authentication auth = getAuthenticatedUser();
        if (auth == null) {
            log.warn("Unauthorized access attempt to addMovie()");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse(false, ApiResponseMessage.UNAUTHORIZED_ACCESS));
        }
    	
        // Validate incoming status against enum-derived allowed codes
        if (status == null || !ALLOWED_STATUS_CODES.contains(status)) {
            log.warn("Invalid status provided for update: {}", status);
            return ResponseEntity.badRequest().body(new CommonApiResponse(false, ApiResponseMessage.INVALID_MOVIE_STATUS_CODE));
        }

        // Find movie
        var movieOpt = movieRepository.findByPublicId(publicId);
        if (movieOpt.isEmpty()) {
            log.warn("Movie not found for publicId: {}", publicId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse(false, ApiResponseMessage.MOVIE_NOT_FOUND));
        }

        Movie movie = movieOpt.get();

        // If same status, return No Content or OK — here we return OK with message
        if (movie.getStatus() == status) {
            String msgSame = ApiResponseMessage.NO_CHANGE + " (status already " + status + ")";
            return ResponseEntity.ok(new CommonApiResponse(true, msgSame));
        }

        // Update status & audit
        movie.setStatus(status);
        
        // updatedAt will be set via @PreUpdate (entity lifecycle)
        movieRepository.save(movie);

        log.info("Movie status updated: publicId={}, newStatus={}", publicId, status);
        String successMsg = ApiResponseMessage.MOVIE_STATUS_UPDATED + " newStatus:" + status;
        return ResponseEntity.ok(new CommonApiResponse(true, successMsg));
    }
    
	@Override
	@Transactional
	public ResponseEntity<CommonApiResponse> updateMovieDetails(String publicId, UpdateMovieRequest request) {
		
		// 1) Ensure authenticated
		Authentication auth = getAuthenticatedUser();

		if (auth == null) {
			log.warn("Unauthorized attempt to update movie details for publicId={}", publicId);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new CommonApiResponse(false, ApiResponseMessage.UNAUTHORIZED_ACCESS));
		}

		// 2) Validate DTO programmatically (controller @Valid already applied but
		// double-check)
		var violations = validator.validate(request);
		
		if (!violations.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (ConstraintViolation<UpdateMovieRequest> v : violations) {
				sb.append(v.getPropertyPath()).append("=").append(v.getMessage()).append("; ");
			}
			log.warn("UpdateMovieRequest validation errors: {}", sb.toString());
			throw new ConstraintViolationException(violations);
		}

		// 3) Load movie
		var movieOpt = movieRepository.findByPublicId(publicId);
		
		if (movieOpt.isEmpty()) {
			log.warn("Movie not found for publicId={}", publicId);
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new CommonApiResponse(false, ApiResponseMessage.MOVIE_NOT_FOUND));
		}

		Movie movie = movieOpt.get();

		// 4) If title is provided and different, check uniqueness
		if (request.getTitle() != null) {
			String newTitle = request.getTitle().trim();
		
			if (!newTitle.equalsIgnoreCase(movie.getTitle()) && movieRepository.existsByTitleIgnoreCase(newTitle)) {
				log.warn("Cannot update movie title to existing title: {}", newTitle);
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(new CommonApiResponse(false, ApiResponseMessage.MOVIE_ALREADY_EXISTS));
			}
			
			movie.setTitle(newTitle);
		}

		// 5) Map optional fields (only update when non-null)
		if (request.getDescription() != null)
			movie.setDescription(trimToNull(request.getDescription()));
		if (request.getDurationMinutes() != null)
			movie.setDurationMinutes(request.getDurationMinutes());
		if (request.getGenres() != null)
			movie.setGenres(trimToNull(request.getGenres()));
		if (request.getLanguage() != null)
			movie.setLanguage(trimToNull(request.getLanguage()));
		if (request.getCountry() != null)
			movie.setCountry(trimToNull(request.getCountry()));
		if (request.getDirector() != null)
			movie.setDirector(trimToNull(request.getDirector()));
		if (request.getCastMembers() != null)
			movie.setCastMembers(trimToNull(request.getCastMembers()));
		if (request.getProducers() != null)
			movie.setProducers(trimToNull(request.getProducers()));
		if (request.getProductionCompany() != null)
			movie.setProductionCompany(trimToNull(request.getProductionCompany()));
		if (request.getDistributor() != null)
			movie.setDistributor(trimToNull(request.getDistributor()));
		if (request.getAudioLanguages() != null)
			movie.setAudioLanguages(trimToNull(request.getAudioLanguages()));
		if (request.getRating() != null)
			movie.setRating(request.getRating());
		if (request.getParentalRating() != null)
			movie.setParentalRating(trimToNull(request.getParentalRating()));
		if (request.getReleaseDate() != null)
			movie.setReleaseDate(request.getReleaseDate());
		if (request.getIs3D() != null)
			movie.setIs3D(request.getIs3D());
		if (request.getIsIMAX() != null)
			movie.setIsIMAX(request.getIsIMAX());
		if (request.getSubtitlesAvailable() != null)
			movie.setSubtitlesAvailable(request.getSubtitlesAvailable());

		// 7) Save
		Movie saved = movieRepository.save(movie);
		log.info("Movie details updated: publicId={}", saved.getPublicId());

		// 8) Response
		return ResponseEntity.ok(new CommonApiResponse(true, ApiResponseMessage.MOVIE_UPDATED));

	}

	@Override
	@Transactional
	public ResponseEntity<CommonApiResponse> updateMoviePoster(String publicId, MultipartFile poster) {
		
		// 1) Ensure caller is authenticated
		Authentication auth = getAuthenticatedUser();
		
		if (auth == null) {
			log.warn("Unauthorized attempt to upload poster for movie {}", publicId);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new CommonApiResponse(false, ApiResponseMessage.UNAUTHORIZED_ACCESS));
		}

		// 2) Basic null/empty check (LocalStorageService will also validate size/type)
		if (poster == null || poster.isEmpty()) {
			log.warn("Empty poster file provided for movie {}", publicId);
			return ResponseEntity.badRequest()
					.body(new CommonApiResponse(false, ApiResponseMessage.MOVIE_POSTER_UPLOAD_FAILED));
		}

		// 3) Fetch movie
		var movieOpt = movieRepository.findByPublicId(publicId);
		
		if (movieOpt.isEmpty()) {
			log.warn("Movie not found while uploading poster: {}", publicId);
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new CommonApiResponse(false, ApiResponseMessage.MOVIE_NOT_FOUND));
		}

		Movie movie = movieOpt.get();

		try {
			// 4) Store file (may throw IllegalArgumentException, IOException,
			// SecurityException)
			String relativePath = localStorageService.store(poster);
			
			if (relativePath == null || relativePath.isBlank()) {
				log.error("LocalStorageService returned empty path for movie {}", publicId);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(new CommonApiResponse(false, ApiResponseMessage.MOVIE_POSTER_UPLOAD_FAILED));
			}

			// 5) Update entity (no updatedBy field)
			movie.setPosterPath(trimToNull(relativePath));
			
			movieRepository.save(movie);

			log.info("Poster updated for movie {}", publicId);

			return ResponseEntity.ok(new CommonApiResponse(true, ApiResponseMessage.MOVIE_POSTER_UPDATED));
		
		} catch (IllegalArgumentException iae) {
			
			// thrown by LocalStorageService for invalid file, too large, invalid content
			// type, etc.
			log.warn("Poster validation failed for movie {}: {}", publicId, iae.getMessage());
			
			String message = iae.getMessage() != null ? iae.getMessage() : ApiResponseMessage.INVALID_POSTER_FILE_TYPE;
			
			return ResponseEntity.badRequest().body(new CommonApiResponse(false, message));
		
		} catch (SecurityException se) {
			log.error("Security error while storing poster for movie " + publicId, se);
			
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new CommonApiResponse(false, ApiResponseMessage.FILE_PATH_TRAVERSAL_DETECTED));
		
		} catch (IOException ioe) {
			
			log.error("I/O error while storing poster for movie " + publicId, ioe);
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new CommonApiResponse(false, ApiResponseMessage.MOVIE_POSTER_UPLOAD_FAILED));
		
		} catch (Exception ex) {
			
			log.error("Unexpected error while uploading poster for movie " + publicId, ex);
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new CommonApiResponse(false, ApiResponseMessage.MOVIE_POSTER_UPLOAD_FAILED));
		
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<MoviesResponse> getMovieByPublicId(String publicId) {
	    
		Authentication auth = getAuthenticatedUser();
	    
		if (auth == null) {
	        log.warn("Unauthorized access to getMovieByPublicId: {}", publicId);
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(MoviesResponse.builder()
	                        .success(false)
	                        .message(ApiResponseMessage.UNAUTHORIZED_ACCESS)
	                        .build());
	    }

	    var movieOpt = movieRepository.findByPublicId(publicId);
	    
	    if (movieOpt.isEmpty()) {
	        log.warn("Movie not found for publicId: {}", publicId);
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(MoviesResponse.builder()
	                        .success(false)
	                        .message(ApiResponseMessage.MOVIE_NOT_FOUND)
	                        .build());
	    }

	    Movie m = movieOpt.get();
	    
	    MovieDto dto = mapToDto(m);

	    MoviesResponse resp = MoviesResponse.builder()
	            .success(true)
	            .message(ApiResponseMessage.FETCH_SUCCESS)
	            .movies(List.of(dto))
	            .build();

	    return ResponseEntity.ok(resp);
	}
	
	
	/**
	 * Retrieves movies by their status.
	 *
	 * Depending on the `paged` flag:
	 * - If `paged = true`: returns paginated movie list.
	 * - If `paged = false`: returns full list (no pagination).
	 *
	 * Includes:
	 * - Authentication check
	 * - Validation for allowed status codes
	 * - Pagination, sorting, and DTO mapping
	 *
	 * @param status the movie status to filter by
	 * @param pageable Spring Data Pageable object (page, size, sort)
	 * @param paged whether pagination is enabled
	 * @return ResponseEntity containing MoviesResponse with movie data
	 */
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<MoviesResponse> getMoviesByStatus(int status, Pageable pageable, boolean paged) {

	    // ✅ Check authentication
	    Authentication auth = getAuthenticatedUser();
	   
	    if (auth == null) {
	        log.warn("Unauthorized access to getMoviesByStatus: {}", status);
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(MoviesResponse.builder()
	                        .success(false)
	                        .message(ApiResponseMessage.UNAUTHORIZED_ACCESS)
	                        .build());
	    }

	    // ✅ Validate movie status
	    if (!ALLOWED_STATUS_CODES.contains(status)) {
	        log.warn("Invalid movie status requested: {}", status);
	        return ResponseEntity.badRequest()
	                .body(MoviesResponse.builder()
	                        .success(false)
	                        .message(ApiResponseMessage.INVALID_MOVIE_STATUS)
	                        .build());
	    }

	    // ✅ If pagination is requested
	    if (paged) {
	        Page<Movie> moviePage = movieRepository.findByStatus(status, pageable);
	        var dtoList = moviePage.getContent().stream()
	                .map(this::mapToDto)
	                .toList();

	        log.info("Fetched paged movies for status {} - page {}/{}", status, moviePage.getNumber(), moviePage.getTotalPages());

	        return ResponseEntity.ok(MoviesResponse.builder()
	                .success(true)
	                .message(ApiResponseMessage.FETCH_SUCCESS)
	                .movies(dtoList)
	                .page(moviePage.getNumber())
	                .size(moviePage.getSize())
	                .totalElements(moviePage.getTotalElements())
	                .totalPages(moviePage.getTotalPages())
	                .build());
	    }

	    // ✅ Non-paged: fetch all movies for the given status
	    var movies = movieRepository.findByStatus(status);
	    var dtoList = movies.stream()
	            .map(this::mapToDto)
	            .toList();

	    log.info("Fetched all movies for status {} - total {}", status, dtoList.size());

	    return ResponseEntity.ok(MoviesResponse.builder()
	            .success(true)
	            .message(ApiResponseMessage.FETCH_SUCCESS)
	            .movies(dtoList)
	            .build());
	    
	}


//	@Override
//	@Transactional(readOnly = true)
//	public ResponseEntity<MoviesResponse> getMoviesByStatus(int status) {
//	    
//		Authentication auth = getAuthenticatedUser();
//	    
//		if (auth == null) {
//	        log.warn("Unauthorized access to getMoviesByStatus: {}", status);
//	    
//	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//	                .body(MoviesResponse.builder()
//	                        .success(false)
//	                        .message(ApiResponseMessage.UNAUTHORIZED_ACCESS)
//	                        .build());
//	    }
//
//	    if (!ALLOWED_STATUS_CODES.contains(status)) {
//	        
//	    	log.warn("Invalid movie status requested: {}", status);
//	        
//	    	return ResponseEntity.badRequest()
//	                .body(MoviesResponse.builder()
//	                        .success(false)
//	                        .message(ApiResponseMessage.INVALID_MOVIE_STATUS)
//	                        .build());
//	    }
//
//	    var movies = movieRepository.findByStatus(status);
//	    
//	    var list = movies.stream().map(this::mapToDto).toList();
//
//	    MoviesResponse resp = MoviesResponse.builder()
//	            .success(true)
//	            .message(ApiResponseMessage.FETCH_SUCCESS)
//	            .movies(list)
//	            .build();
//
//	    return ResponseEntity.ok(resp);
//	}
//	
//	@Override
//	@Transactional(readOnly = true)
//	public ResponseEntity<MoviesResponse> getMoviesByStatusPaged(int status, Pageable pageable) {
//	    
//		// auth check
//	    Authentication auth = getAuthenticatedUser();
//	    
//	    if (auth == null) {
//	    	log.warn("Unauthorized access to getMoviesByStatusPaged: {}", status);
//	    
//	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//	                .body(MoviesResponse.builder()
//	                        .success(false)
//	                        .message(ApiResponseMessage.UNAUTHORIZED_ACCESS)
//	                        .build());
//	    }
//
//	    // validate status code
//	    if (!ALLOWED_STATUS_CODES.contains(status)) {
//	        log.warn("Invalid status requested (paged): {}", status);
//	        
//	        return ResponseEntity.badRequest()
//	                .body(MoviesResponse.builder()
//	                        .success(false)
//	                        .message(ApiResponseMessage.INVALID_MOVIE_STATUS)
//	                        .build());
//	    }
//
//	    Page<Movie> page = movieRepository.findByStatus(status, pageable);
//
//	    var dtoList = page.getContent().stream()
//	            .map(this::mapToDto)
//	            .toList();
//
//	    MoviesResponse resp = MoviesResponse.builder()
//	            .success(true)
//	            .message(ApiResponseMessage.FETCH_SUCCESS)
//	            .movies(dtoList)
//	            .page(page.getNumber())
//	            .size(page.getSize())
//	            .totalElements(page.getTotalElements())
//	            .totalPages(page.getTotalPages())
//	            .build();
//
//	    return ResponseEntity.ok(resp);
//	
//	}
	
	// ---------------- paginated list ----------------
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<MoviesResponse> getMovies(Pageable pageable, Integer status, String search) {
		// auth check
		
		Authentication auth = getAuthenticatedUser();
		
		if (auth == null) {
			log.warn("Unauthorized access to getMovies");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
					MoviesResponse.builder().success(false).message(ApiResponseMessage.UNAUTHORIZED_ACCESS).build());
		}

		// validate status if provided
		if (status != null && !ALLOWED_STATUS_CODES.contains(status)) {
			return ResponseEntity.badRequest().body(
					MoviesResponse.builder().success(false).message(ApiResponseMessage.INVALID_MOVIE_STATUS).build());
		}

		Page<Movie> page;
	
		if (search != null && !search.isBlank()) {
			if (status != null) {
				page = movieRepository.findByTitleContainingIgnoreCaseAndStatus(search.trim(), status, pageable);
			} else {
				page = movieRepository.findByTitleContainingIgnoreCase(search.trim(), pageable);
			}
		} else {
			if (status != null) {
				page = movieRepository.findByStatus(status, pageable);
			} else {
				page = movieRepository.findAll(pageable);
			}
		}

	    var dtos = page.getContent().stream().map(this::mapToDto).toList();

	    MoviesResponse resp = MoviesResponse.builder()
	            .success(true)
	            .message(ApiResponseMessage.FETCH_SUCCESS)
	            .movies(dtos)
	            .page(page.getNumber())
	            .size(page.getSize())
	            .totalElements(page.getTotalElements())
	            .totalPages(page.getTotalPages())
	            .build();

	    return ResponseEntity.ok(resp);
	}

	// ---------------- non-paged list ----------------
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<MoviesResponse> getMoviesNoPage(Integer status, String search) {
		
		Authentication auth = getAuthenticatedUser();
		
		if (auth == null) {
			log.warn("Unauthorized access to getMoviesNoPage");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
					MoviesResponse.builder().success(false).message(ApiResponseMessage.UNAUTHORIZED_ACCESS).build());
		}

		if (status != null && !ALLOWED_STATUS_CODES.contains(status)) {
			return ResponseEntity.badRequest().body(
					MoviesResponse.builder().success(false).message(ApiResponseMessage.INVALID_MOVIE_STATUS).build());
		}

		List<Movie> list;
		
		if (search != null && !search.isBlank()) {
			if (status != null) {
				list = movieRepository.findByTitleContainingIgnoreCaseAndStatus(search.trim(), status);
			} else {
				list = movieRepository.findByTitleContainingIgnoreCase(search.trim());
			}
		} else {
			if (status != null) {
				list = movieRepository.findByStatus(status);
			} else {
				list = movieRepository.findAll();
			}
		}

		var dtos = list.stream().map(this::mapToDto).toList();

	    MoviesResponse resp = MoviesResponse.builder()
	            .success(true)
	            .message(ApiResponseMessage.FETCH_SUCCESS)
	            .movies(dtos)
	            .build();

	    return ResponseEntity.ok(resp);
	
	}

	// ---------------- poster serving ----------------
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<Resource> getMoviePoster(String publicId) {
	    
		Authentication auth = getAuthenticatedUser();
	    
		if (auth == null) {
	        log.warn("Unauthorized access to getMoviePoster: {}", publicId);
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	    }

	    var movieOpt = movieRepository.findByPublicId(publicId);
	    
	    if (movieOpt.isEmpty()) {
	        log.warn("Movie not found when requesting poster: {}", publicId);
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	    }

	    Movie movie = movieOpt.get();
	    
	    String posterPath = movie.getPosterPath();
	   
	    try {
	        Resource resource = localStorageService.loadAsResource(posterPath);
	        if (resource == null || !resource.exists()) {
	            log.warn("Poster resource not found for movie {} path={}", publicId, posterPath);
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	        }

	        // attempt to determine content type
	        String contentType = null;
	        try {
	            // prefer filename-based guess
	            contentType = URLConnection.guessContentTypeFromName(resource.getFilename());
	        } catch (Exception ignored) {}

	        if (contentType == null) {
	            contentType = "application/octet-stream";
	        }

	        return ResponseEntity.ok()
	                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
	                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
	                .body(resource);

	    } catch (IOException ioe) {
	        log.error("Error loading poster resource for movie " + publicId, ioe);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	    } catch (Exception ex) {
	        log.error("Unexpected error loading poster for movie " + publicId, ex);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	    }
	}

	

    // ---------- helpers ----------
	
	private MovieDto mapToDto(Movie m) {
	    return MovieDto.builder()
	            .publicId(m.getPublicId())
	            .title(m.getTitle())
	            .description(m.getDescription())
	            .durationMinutes(m.getDurationMinutes())
	            .genres(m.getGenres())
	            .language(m.getLanguage())
	            .country(m.getCountry())
	            .director(m.getDirector())
	            .castMembers(m.getCastMembers())
	            .producers(m.getProducers())
	            .productionCompany(m.getProductionCompany())
	            .distributor(m.getDistributor())
	            .posterPath(m.getPosterPath())
	            .is3D(m.getIs3D())
	            .isIMAX(m.getIsIMAX())
	            .subtitlesAvailable(m.getSubtitlesAvailable())
	            .audioLanguages(m.getAudioLanguages())
	            .rating(m.getRating())
	            .parentalRating(m.getParentalRating())
	            .releaseDate(m.getReleaseDate())
	            .status(m.getStatus())
	            .createdAt(m.getCreatedAt())
	            .build();
	}
	
    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
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
    
    /**
     * Ensures the current user is authenticated.
     * Returns the Authentication object if valid; otherwise returns null.
     */
    private Authentication getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        return auth;
    }

}
