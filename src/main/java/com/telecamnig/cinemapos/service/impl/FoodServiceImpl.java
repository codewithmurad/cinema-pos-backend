package com.telecamnig.cinemapos.service.impl;

import java.io.IOException;
import java.net.URLConnection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.telecamnig.cinemapos.dto.AddFoodRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.FoodAdminResponse;
import com.telecamnig.cinemapos.dto.FoodCounterResponse;
import com.telecamnig.cinemapos.dto.UpdateFoodRequest;
import com.telecamnig.cinemapos.entity.Food;
import com.telecamnig.cinemapos.entity.FoodCategory;
import com.telecamnig.cinemapos.repository.FoodCategoryRepository;
import com.telecamnig.cinemapos.repository.FoodRepository;
import com.telecamnig.cinemapos.service.FoodService;
import com.telecamnig.cinemapos.storage.LocalStorageService;
import com.telecamnig.cinemapos.storage.StorageService;
import com.telecamnig.cinemapos.utility.Constants.FoodCategoryStatus;
import com.telecamnig.cinemapos.utility.Constants.FoodStatus;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class FoodServiceImpl implements FoodService {

    private static final Logger log = LoggerFactory.getLogger(FoodServiceImpl.class);

    private final FoodRepository foodRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final LocalStorageService localStorageService;
    private final Validator validator;

    public FoodServiceImpl(
            FoodRepository foodRepository,
            FoodCategoryRepository foodCategoryRepository,
            LocalStorageService localStorageService,
            Validator validator) {

        this.foodRepository = foodRepository;
        this.foodCategoryRepository = foodCategoryRepository;
        this.localStorageService = localStorageService;
        this.validator = validator;
    }

    @Override
    @Transactional
    public ResponseEntity<CommonApiResponse> addFood(
            AddFoodRequest request,
            MultipartFile imageFile) {

        log.info("AddFood request received: name={}", request.getName());

        // ==================== STEP 1: Defensive validation ====================
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        // ==================== STEP 2: Resolve category ====================
        FoodCategory category = foodCategoryRepository
                .findByPublicId(request.getCategoryPublicId())
                .orElse(null);

        if (category == null) {
            return ResponseEntity.badRequest()
                    .body(new CommonApiResponse(false, "Invalid food category"));
        }

        if (category.getStatus() != FoodCategoryStatus.ACTIVE.getCode()) {
            return ResponseEntity.badRequest()
                    .body(new CommonApiResponse(false, "Food category is not active"));
        }

        // ==================== STEP 3: Name uniqueness inside category ====================
        String name = request.getName().trim();
        if (foodRepository.existsByNameAndCategory(name, category.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CommonApiResponse(false, "Food already exists in this category"));
        }

        // ==================== STEP 4: Handle image upload ====================
        String imagePath = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                imagePath = localStorageService.store(imageFile, StorageService.StorageType.FOODS);
            } catch (IOException e) {
                log.error("Failed to upload food image", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new CommonApiResponse(false, "Failed to upload food image"));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(new CommonApiResponse(false, e.getMessage()));
            }
        }

        // ==================== STEP 5: Map DTO → Entity ====================
        Food food = new Food();
        food.setName(name);
        food.setDescription(trimToNull(request.getDescription()));
        food.setPrice(request.getPrice());
        food.setDisplayOrder(request.getDisplayOrder());
        food.setImagePath(imagePath);

        food.setCategoryId(category.getId());
        food.setCategoryPublicId(category.getPublicId());

        // System defaults
        food.setStatus(FoodStatus.ACTIVE.getCode());

        // Audit
        Long currentUserId = extractCurrentUserId();
        if (currentUserId != null) {
            food.setCreatedBy(currentUserId);
        }

        Food saved = foodRepository.save(food);

        log.info("Food created successfully: id={}, publicId={}, name={}",
                saved.getId(), saved.getPublicId(), saved.getName());

        String msg = "Food created successfully. publicId:" + saved.getPublicId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonApiResponse(true, msg));
    }
    
    @Override
    @Transactional
    public ResponseEntity<CommonApiResponse> updateFood(
            String publicId,
            UpdateFoodRequest request,
            MultipartFile imageFile) {

        log.info("UpdateFood request received: publicId={}", publicId);

        // ==================== STEP 1: Defensive validation ====================
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        // ==================== STEP 2: Fetch food ====================
        Food food = foodRepository.findByPublicId(publicId).orElse(null);
        if (food == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse(false, "Food not found"));
        }

        // ==================== STEP 3: Resolve category ====================
        FoodCategory category = foodCategoryRepository
                .findByPublicId(request.getCategoryPublicId())
                .orElse(null);

        if (category == null) {
            return ResponseEntity.badRequest()
                    .body(new CommonApiResponse(false, "Invalid food category"));
        }

        if (category.getStatus() != FoodCategoryStatus.ACTIVE.getCode()) {
            return ResponseEntity.badRequest()
                    .body(new CommonApiResponse(false, "Food category is not active"));
        }

        // ==================== STEP 4: Name uniqueness inside category ====================
        String newName = request.getName().trim();
        boolean categoryChanged = !food.getCategoryId().equals(category.getId());
        boolean nameChanged = !food.getName().equalsIgnoreCase(newName);

        if ((categoryChanged || nameChanged)
                && foodRepository.existsByNameAndCategory(newName, category.getId())) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CommonApiResponse(false, "Food already exists in this category"));
        }

        // ==================== STEP 5: Handle image update ====================
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String newImagePath = localStorageService.store(imageFile, StorageService.StorageType.FOODS);
                food.setImagePath(newImagePath);
            } catch (IOException e) {
                log.error("Failed to upload food image", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new CommonApiResponse(false, "Failed to upload food image"));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(new CommonApiResponse(false, e.getMessage()));
            }
        }

        // ==================== STEP 6: Update fields ====================
        food.setName(newName);
        food.setDescription(trimToNull(request.getDescription()));
        food.setPrice(request.getPrice());
        food.setDisplayOrder(request.getDisplayOrder());

        food.setCategoryId(category.getId());
        food.setCategoryPublicId(category.getPublicId());

        foodRepository.save(food);

        log.info("Food updated successfully: publicId={}", publicId);

        return ResponseEntity.ok(
                new CommonApiResponse(true, "Food updated successfully")
        );
    }
    
    @Override
    @Transactional
    public ResponseEntity<CommonApiResponse> updateFoodStatus(
            String publicId,
            Integer status) {

        log.info("UpdateFoodStatus request: publicId={}, status={}", publicId, status);

        // ==================== STEP 1: Validate status ====================
        if (status == null ||
            status < FoodStatus.INACTIVE.getCode() ||
            status > FoodStatus.DELETED.getCode()) {

            return ResponseEntity.badRequest()
                    .body(new CommonApiResponse(false, "Invalid food status"));
        }

        // ==================== STEP 2: Fetch food ====================
        Food food = foodRepository.findByPublicId(publicId).orElse(null);
        if (food == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse(false, "Food not found"));
        }

        // ==================== STEP 3: Prevent redundant update ====================
        if (food.getStatus() == status) {
            return ResponseEntity.ok(
                    new CommonApiResponse(true, "Food status already set")
            );
        }

        // ==================== STEP 4: Apply status ====================
        food.setStatus(status);
        foodRepository.save(food);

        log.info("Food status updated successfully: publicId={}, newStatus={}",
                publicId, status);

        return ResponseEntity.ok(
                new CommonApiResponse(true, "Food status updated successfully")
        );
    
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<FoodCounterResponse>> fetchFoodsForCounter(
            String categoryPublicId,
            String search) {

        Long categoryId = null;

        // ==================== Resolve category if provided ====================
        if (categoryPublicId != null && !categoryPublicId.isBlank()) {
            FoodCategory category = foodCategoryRepository
                    .findByPublicId(categoryPublicId)
                    .orElse(null);

            if (category == null || category.getStatus() != FoodCategoryStatus.ACTIVE.getCode()) {
                return ResponseEntity.badRequest().build();
            }
            categoryId = category.getId();
        }

        // normalize search
        String keyword = (search == null || search.isBlank())
                ? null
                : search.trim();

        List<Food> foods = foodRepository.findFoodsForCounter(
                FoodStatus.ACTIVE.getCode(),
                categoryId,
                keyword
        );

        List<FoodCounterResponse> response = foods.stream()
                .map(f -> FoodCounterResponse.builder()
                        .publicId(f.getPublicId())
                        .name(f.getName())
                        .price(f.getPrice())
                        .imagePath(f.getImagePath())
                        .displayOrder(f.getDisplayOrder())
                        .categoryPublicId(f.getCategoryPublicId())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<FoodAdminResponse>> getFoodsByStatus(
            Integer status,
            String categoryPublicId,
            String search) {

        if (status == null || status < 0 || status > 2) {
            return ResponseEntity.badRequest().build();
        }

        Long categoryId = null;
        if (categoryPublicId != null && !categoryPublicId.isBlank()) {
            FoodCategory category = foodCategoryRepository
                    .findByPublicId(categoryPublicId)
                    .orElse(null);
            if (category == null) {
                return ResponseEntity.badRequest().build();
            }
            categoryId = category.getId();
        }

        String keyword = (search == null || search.isBlank())
                ? null
                : search.trim();

        List<Food> foods = foodRepository.findByStatusForAdmin(
                status, categoryId, keyword
        );

        List<FoodAdminResponse> response = foods.stream()
                .map(f -> FoodAdminResponse.builder()
                        .publicId(f.getPublicId())
                        .name(f.getName())
                        .price(f.getPrice())
                        .imagePath(f.getImagePath())
                        .categoryPublicId(f.getCategoryPublicId())
                        .status(f.getStatus())
                        .displayOrder(f.getDisplayOrder())
                        .updatedAt(f.getUpdatedAt())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getFoodImage(String publicId) {
        
        log.info("Requesting food image for publicId: {}", publicId);
        
        // ==================== STEP 1: Fetch food ====================
        var foodOpt = foodRepository.findByPublicId(publicId);
        
        if (foodOpt.isEmpty()) {
            log.warn("Food not found when requesting image: {}", publicId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Food food = foodOpt.get();
        
        // ==================== STEP 2: Check if food has an image ====================
        String imagePath = food.getImagePath();
        
        if (imagePath == null || imagePath.trim().isEmpty()) {
            log.warn("Food {} has no image path", publicId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        // ==================== STEP 3: Check food status (optional security) ====================
        // Only allow image access for ACTIVE or INACTIVE foods, not DELETED
        if (food.getStatus() == FoodStatus.DELETED.getCode()) {
            log.warn("Attempted to access image for DELETED food: {}", publicId);
            return ResponseEntity.status(HttpStatus.GONE).build(); // 410 GONE
        }
        
        // ==================== STEP 4: Load and serve the image ====================
        try {
            Resource resource = localStorageService.loadAsResource(
                imagePath, 
                StorageService.StorageType.FOODS
            );
            
            if (resource == null || !resource.exists()) {
                log.warn("Food image resource not found for food {} path={}", 
                    publicId, imagePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Attempt to determine content type from filename
            String contentType = null;
            try {
                contentType = URLConnection.guessContentTypeFromName(resource.getFilename());
            } catch (Exception ignored) {
                log.debug("Could not guess content type from filename for food: {}", publicId);
            }

            // Fallback to default if content type could not be determined
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.info("Successfully loading food image for {}: {}", publicId, resource.getFilename());
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"" + resource.getFilename() + "\""
                    )
                    .body(resource);

        } catch (IOException ioe) {
            log.error("Error loading food image resource for food {}", publicId, ioe);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception ex) {
            log.error("Unexpected error loading image for food {}", publicId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== Utility ====================

    private String trimToNull(String val) {
        if (val == null) return null;
        String t = val.trim();
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
}
