package com.telecamnig.cinemapos.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.telecamnig.cinemapos.dto.AddFoodCategoryRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.FoodCategoryResponse;
import com.telecamnig.cinemapos.dto.UpdateFoodCategoryRequest;
import com.telecamnig.cinemapos.dto.UpdateFoodCategoryStatusRequest;
import com.telecamnig.cinemapos.service.FoodCategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/food-category")
public class FoodCategoryController {

    private final FoodCategoryService foodCategoryService;

    public FoodCategoryController(FoodCategoryService foodCategoryService) {
        this.foodCategoryService = foodCategoryService;
    }
    
    /**
     * Add a new food category with optional image.
     * POST /api/v1/food-category/add
     */
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonApiResponse> addFoodCategory(
            @Valid @RequestPart("categoryData") AddFoodCategoryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        return foodCategoryService.addFoodCategory(request, imageFile);
    }

    /**
     * Update food category details and optional image.
     * PUT /api/v1/food-category/{publicId}/update
     */
    @PutMapping(value = "/{publicId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonApiResponse> updateFoodCategory(
            @PathVariable("publicId") String publicId,
            @Valid @RequestPart("categoryData") UpdateFoodCategoryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        return foodCategoryService.updateFoodCategory(publicId, request, imageFile);
    }
    
    /**
     * Fetch ACTIVE food categories for Food Counter UI.
     * GET /api/v1/food-category/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<FoodCategoryResponse>> getActiveFoodCategories() {
        return foodCategoryService.getActiveFoodCategories();
    }
    
    /**
     * Update food category status (Admin only).
     * PUT /api/v1/food-category/{publicId}/status
     */
    @PutMapping("/{publicId}/status")
    public ResponseEntity<CommonApiResponse> updateFoodCategoryStatus(
            @PathVariable("publicId") String publicId,
            @Valid @RequestBody UpdateFoodCategoryStatusRequest request) {

        return foodCategoryService.updateFoodCategoryStatus(publicId, request.getStatus());
    }
    
    /**
     * Serve food category image.
     * GET /api/v1/food-category/{publicId}/image
     */
    @GetMapping("/{publicId}/image")
    public ResponseEntity<Resource> getFoodCategoryImage(@PathVariable("publicId") String publicId) {
        return foodCategoryService.getFoodCategoryImage(publicId);
    }
}
