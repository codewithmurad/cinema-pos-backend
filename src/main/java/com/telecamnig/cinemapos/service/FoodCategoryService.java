package com.telecamnig.cinemapos.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.telecamnig.cinemapos.dto.AddFoodCategoryRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.FoodCategoryResponse;
import com.telecamnig.cinemapos.dto.UpdateFoodCategoryRequest;

public interface FoodCategoryService {

	ResponseEntity<CommonApiResponse> addFoodCategory(AddFoodCategoryRequest request, MultipartFile imageFile);

	ResponseEntity<CommonApiResponse> updateFoodCategory(String publicId, UpdateFoodCategoryRequest request,
			MultipartFile imageFile);

	ResponseEntity<List<FoodCategoryResponse>> getActiveFoodCategories();

	ResponseEntity<CommonApiResponse> updateFoodCategoryStatus(String publicId, Integer status);
	
	ResponseEntity<Resource> getFoodCategoryImage(String publicId);

}
