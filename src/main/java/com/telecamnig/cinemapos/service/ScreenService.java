package com.telecamnig.cinemapos.service;

import org.springframework.http.ResponseEntity;

import com.telecamnig.cinemapos.dto.ScreenDetailResponse;
import com.telecamnig.cinemapos.dto.ScreenListResponse;

public interface ScreenService {

	ResponseEntity<ScreenListResponse> getAllActiveScreens();

	ResponseEntity<ScreenListResponse> getAllScreens();

    ResponseEntity<ScreenDetailResponse> getScreenByPublicId(String publicId);
    
	ResponseEntity<ScreenDetailResponse> getScreenByCode(String code);

}