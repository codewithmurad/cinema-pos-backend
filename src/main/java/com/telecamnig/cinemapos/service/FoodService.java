package com.telecamnig.cinemapos.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.telecamnig.cinemapos.dto.AddFoodRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.FoodAdminResponse;
import com.telecamnig.cinemapos.dto.FoodCounterResponse;
import com.telecamnig.cinemapos.dto.UpdateFoodRequest;

public interface FoodService {

	ResponseEntity<CommonApiResponse> addFood(AddFoodRequest request, MultipartFile imageFile);

	ResponseEntity<CommonApiResponse> updateFood(String publicId, UpdateFoodRequest request, MultipartFile imageFile);

	ResponseEntity<CommonApiResponse> updateFoodStatus(String publicId, Integer status);

	ResponseEntity<List<FoodCounterResponse>> fetchFoodsForCounter(String categoryPublicId, String search);

	ResponseEntity<List<FoodAdminResponse>> getFoodsByStatus(
	        Integer status,
	        String categoryPublicId,
	        String search
	);
	
	ResponseEntity<Resource> getFoodImage(String publicId);

}
