package com.telecamnig.cinemapos.service;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.telecamnig.cinemapos.dto.AddMovieRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.MoviesResponse;
import com.telecamnig.cinemapos.dto.UpdateMovieRequest;

public interface MovieService {

    /**
     * Add a new movie and return ResponseEntity containing CommonApiResponse.
     * Controller will forward this ResponseEntity to the client.
     */
	ResponseEntity<CommonApiResponse> addMovie(AddMovieRequest request, MultipartFile posterFile);
    
    /**
     * Update movie status by publicId.
     * Returns ResponseEntity with CommonApiResponse so controller can forward directly.
     */
    ResponseEntity<CommonApiResponse> updateMovieStatus(String publicId, Integer status);
    
    /**
     * Update movie details (excluding poster).
     */
    ResponseEntity<CommonApiResponse> updateMovieDetails(String publicId, UpdateMovieRequest request);
    
    /**
     * Update the poster for a movie identified by publicId.
     * The service returns ResponseEntity<CommonApiResponse> so controller can forward directly.
     *
     * @param publicId external movie id
     * @param poster multipart file uploaded by client
     */
    ResponseEntity<CommonApiResponse> updateMoviePoster(String publicId, MultipartFile poster);

    ResponseEntity<MoviesResponse> getMovieByPublicId(String publicId);

//    ResponseEntity<MoviesResponse> getMoviesByStatus(int status);
//    
//    ResponseEntity<MoviesResponse> getMoviesByStatusPaged(int status, Pageable pageable);
    
    ResponseEntity<MoviesResponse> getMoviesByStatus(int status, Pageable pageable, boolean paged);
    
    ResponseEntity<MoviesResponse> getMovies(Pageable pageable, Integer status, String search);

    ResponseEntity<MoviesResponse> getMoviesNoPage(Integer status, String search);

    ResponseEntity<Resource> getMoviePoster(String publicId);



}
