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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.telecamnig.cinemapos.dto.AddFoodRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.FoodAdminResponse;
import com.telecamnig.cinemapos.dto.FoodCounterResponse;
import com.telecamnig.cinemapos.dto.UpdateFoodRequest;
import com.telecamnig.cinemapos.dto.UpdateFoodStatusRequest;
import com.telecamnig.cinemapos.service.FoodService;

import jakarta.validation.Valid;

/**
 * FoodController
 *
 * Handles all Food-related operations in the Cinema POS system.
 *
 * This controller is intentionally split by responsibility:
 *
 * 1) ADMIN APIs
 *    - Add Food
 *    - Update Food
 *    - Activate / Deactivate / Delete Food
 *    - Fetch foods by status (availability management)
 *
 * 2) POS COUNTER APIs
 *    - Fetch foods for counter screen (category + search)
 *
 * DESIGN NOTES:
 * - Status changes are handled via a dedicated API (NOT via update)
 * - Image upload is optional during update
 * - No pagination is used for food lists (dataset is small in POS)
 * - Soft delete is enforced via status field
 *
 * This design keeps the system:
 * - Fast
 * - Safe
 * - Easy to maintain
 * - Suitable for offline POS usage
 */
@RestController
@RequestMapping("/api/v1/food")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    /**
     * ===========================
     * ADMIN APIs
     * ===========================
     */

    /**
     * Add a new food item.
     *
     * Usage:
     * - Admin creates a food item under a category
     * - Image is REQUIRED at creation time
     *
     * Endpoint:
     * POST /api/v1/food/add
     *
     * Notes:
     * - Food status is system-controlled (default ACTIVE)
     * - Category must exist and be ACTIVE
     * - Image is stored on local filesystem (offline POS support)
     */
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonApiResponse> addFood(
            @Valid @RequestPart("foodData") AddFoodRequest request,
            @RequestPart(value = "image", required = true) MultipartFile imageFile) {

        return foodService.addFood(request, imageFile);
    }

    /**
     * Update food details.
     *
     * Usage:
     * - Admin updates name, price, category, or display order
     * - Image update is OPTIONAL
     *
     * Endpoint:
     * PUT /api/v1/food/{publicId}/update
     *
     * Notes:
     * - If image is not provided, existing image remains unchanged
     * - Status is NOT updated here (handled via separate API)
     */
    @PutMapping(value = "/{publicId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonApiResponse> updateFood(
            @PathVariable("publicId") String publicId,
            @Valid @RequestPart("foodData") UpdateFoodRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        return foodService.updateFood(publicId, request, imageFile);
    }

    /**
     * Update food availability status.
     *
     * Usage:
     * - Admin activates / deactivates food items based on availability
     * - Soft delete is performed using status = DELETED
     *
     * Endpoint:
     * PUT /api/v1/food/{publicId}/status
     *
     * Status Codes:
     * 0 = INACTIVE
     * 1 = ACTIVE
     * 2 = DELETED
     *
     * Notes:
     * - No hard delete is allowed
     * - Counter will only show ACTIVE foods
     */
    @PutMapping("/{publicId}/status")
    public ResponseEntity<CommonApiResponse> updateFoodStatus(
            @PathVariable("publicId") String publicId,
            @Valid @RequestBody UpdateFoodStatusRequest request) {

        return foodService.updateFoodStatus(publicId, request.getStatus());
    }

    /**
     * Fetch foods by status (Admin view).
     *
     * Usage:
     * - Admin dashboard
     * - Availability management
     *
     * Endpoint:
     * GET /api/v1/food/status/{status}
     *
     * Optional Filters:
     * - categoryPublicId → filter foods by category
     * - search → search by food name
     *
     * Notes:
     * - No pagination is used (food list is small)
     * - Returns admin-level response DTO
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FoodAdminResponse>> getFoodsByStatus(
            @PathVariable("status") Integer status,
            @RequestParam(value = "categoryPublicId", required = false) String categoryPublicId,
            @RequestParam(value = "search", required = false) String search) {

        return foodService.getFoodsByStatus(status, categoryPublicId, search);
    }

    /**
     * ===========================
     * POS COUNTER APIs
     * ===========================
     */

    /**
     * Fetch foods for POS counter screen.
     *
     * Usage:
     * - Cashier selects a category
     * - Optional search by food name
     *
     * Endpoint:
     * GET /api/v1/food/counter
     *
     * Behavior:
     * - Returns only ACTIVE foods
     * - Ordered by displayOrder, then name
     * - No pagination (fast UI rendering)
     *
     * Notes:
     * - This API is performance-critical
     * - Designed for offline POS usage
     */
    @GetMapping("/counter")
    public ResponseEntity<List<FoodCounterResponse>> fetchFoodsForCounter(
            @RequestParam(value = "categoryPublicId", required = false) String categoryPublicId,
            @RequestParam(value = "search", required = false) String search) {

        return foodService.fetchFoodsForCounter(categoryPublicId, search);
    }
    
    /**
     * Serve food image.
     * 
     * Usage:
     * - Display food image in POS counter
     * - Display food image in mobile apps
     * - Admin preview
     *
     * Endpoint:
     * GET /api/v1/food/{publicId}/image
     *
     * Behavior:
     * - Returns food image as byte stream
     * - Proper content-type headers
     * - Checks food status (won't serve DELETED foods)
     *
     * Notes:
     * - Image path is stored in food.imagePath
     * - Uses FOODS storage type
     */
    @GetMapping("/{publicId}/image")
    public ResponseEntity<Resource> getFoodImage(@PathVariable("publicId") String publicId) {
        return foodService.getFoodImage(publicId);
    }

}
