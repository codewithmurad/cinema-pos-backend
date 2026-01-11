package com.telecamnig.cinemapos.service.impl;

import java.io.IOException;
import java.net.URLConnection;
import java.util.List;
import java.util.Set;

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

import com.telecamnig.cinemapos.dto.AddFoodCategoryRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.FoodCategoryResponse;
import com.telecamnig.cinemapos.dto.UpdateFoodCategoryRequest;
import com.telecamnig.cinemapos.entity.FoodCategory;
import com.telecamnig.cinemapos.repository.FoodCategoryRepository;
import com.telecamnig.cinemapos.service.FoodCategoryService;
import com.telecamnig.cinemapos.storage.LocalStorageService;
import com.telecamnig.cinemapos.storage.StorageService;
import com.telecamnig.cinemapos.utility.Constants.FoodCategoryStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class FoodCategoryServiceImpl implements FoodCategoryService {

    private static final Logger log = LoggerFactory.getLogger(FoodCategoryServiceImpl.class);

    private final FoodCategoryRepository foodCategoryRepository;
    private final StorageService storageService;
    private final Validator validator;

    public FoodCategoryServiceImpl(
            FoodCategoryRepository foodCategoryRepository,
            LocalStorageService localStorageService,
            Validator validator) {

        this.foodCategoryRepository = foodCategoryRepository;
        this.storageService = localStorageService;
        this.validator = validator;
    }

    @Override
    @Transactional
    public ResponseEntity<CommonApiResponse> addFoodCategory(
            AddFoodCategoryRequest request,
            MultipartFile imageFile) {

        log.info("AddFoodCategory request received: name={}", request.getName());

        // ==================== STEP 1: Defensive validation ====================
        Set<ConstraintViolation<AddFoodCategoryRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            log.warn("Validation failed for AddFoodCategoryRequest: {}", violations);
            throw new ConstraintViolationException(violations);
        }

        // ==================== STEP 2: Name uniqueness check ====================
        String name = request.getName().trim();
        if (foodCategoryRepository.existsByNameIgnoreCase(name)) {
            log.warn("Duplicate food category name attempted: {}", name);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CommonApiResponse(false, "Food category already exists"));
        }

        // ==================== STEP 3: Handle image upload ====================
        String imagePath = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                imagePath = storageService.store(imageFile, StorageService.StorageType.FOOD_CATEGORIES);
                log.info("Food category image stored successfully: {}", imagePath);
            } catch (IOException e) {
                log.error("Failed to upload food category image", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new CommonApiResponse(false, "Failed to upload category image"));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid image file for food category: {}", e.getMessage());
                return ResponseEntity.badRequest()
                        .body(new CommonApiResponse(false, e.getMessage()));
            } catch (SecurityException e) {
                log.error("Security exception during image upload", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new CommonApiResponse(false, "Security violation during image upload"));
            }
        }

        // ==================== STEP 4: Map DTO → Entity ====================
        FoodCategory category = new FoodCategory();
        category.setName(name);
        category.setDescription(trimToNull(request.getDescription()));
        category.setDisplayOrder(request.getDisplayOrder());
        category.setImagePath(imagePath);

        // System-managed defaults
        category.setStatus(FoodCategoryStatus.ACTIVE.getCode());

        // Audit
        Long currentUserId = extractCurrentUserId();
        if (currentUserId != null) {
            category.setCreatedBy(currentUserId);
        }

        // ==================== STEP 5: Persist ====================
        FoodCategory saved = foodCategoryRepository.save(category);

        log.info("FoodCategory created successfully: id={}, publicId={}, name={}",
                saved.getId(), saved.getPublicId(), saved.getName());

        String msg = "Food category created successfully. publicId:" + saved.getPublicId();
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonApiResponse(true, msg));
    }

    @Override
    @Transactional
    public ResponseEntity<CommonApiResponse> updateFoodCategory(
            String publicId,
            UpdateFoodCategoryRequest request,
            MultipartFile imageFile) {

        log.info("UpdateFoodCategory request: publicId={}", publicId);

        // ==================== STEP 1: Validate request ====================
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        // ==================== STEP 2: Fetch category ====================
        FoodCategory category = foodCategoryRepository.findByPublicId(publicId)
                .orElse(null);

        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse(false, "Food category not found"));
        }

        // ==================== STEP 3: Name uniqueness (excluding self) ====================
        String newName = request.getName().trim();
        if (!category.getName().equalsIgnoreCase(newName)
                && foodCategoryRepository.existsByNameIgnoreCase(newName)) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CommonApiResponse(false, "Food category name already exists"));
        }

        // ==================== STEP 4: Handle image update ====================
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String newImagePath = storageService.store(imageFile, StorageService.StorageType.FOOD_CATEGORIES);
                category.setImagePath(newImagePath);
            } catch (IOException e) {
                log.error("Failed to upload category image", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new CommonApiResponse(false, "Failed to upload category image"));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(new CommonApiResponse(false, e.getMessage()));
            }
        }

        // ==================== STEP 5: Update fields ====================
        category.setName(newName);
        category.setDescription(trimToNull(request.getDescription()));
        category.setDisplayOrder(request.getDisplayOrder());

        foodCategoryRepository.save(category);

        log.info("FoodCategory updated successfully: publicId={}", publicId);

        return ResponseEntity.ok(
                new CommonApiResponse(true, "Food category updated successfully")
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<FoodCategoryResponse>> getActiveFoodCategories() {

        List<FoodCategory> categories =
                foodCategoryRepository.findActiveCategoriesForCounter(
                        FoodCategoryStatus.ACTIVE.getCode()
                );

        List<FoodCategoryResponse> response = categories.stream()
                .map(fc -> FoodCategoryResponse.builder()
                        .publicId(fc.getPublicId())
                        .name(fc.getName())
                        .imagePath(fc.getImagePath())
                        .displayOrder(fc.getDisplayOrder())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<CommonApiResponse> updateFoodCategoryStatus(
            String publicId, Integer status) {

        log.info("UpdateFoodCategoryStatus request: publicId={}, status={}", publicId, status);

        // ==================== STEP 1: Validate status ====================
        if (status == null ||
            status < FoodCategoryStatus.INACTIVE.getCode() ||
            status > FoodCategoryStatus.DELETED.getCode()) {

            return ResponseEntity.badRequest()
                    .body(new CommonApiResponse(false, "Invalid food category status"));
        }

        // ==================== STEP 2: Fetch category ====================
        FoodCategory category = foodCategoryRepository.findByPublicId(publicId)
                .orElse(null);

        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse(false, "Food category not found"));
        }

        // ==================== STEP 3: Prevent redundant update ====================
        if (category.getStatus() == status) {
            return ResponseEntity.ok(
                    new CommonApiResponse(true, "Food category status already set")
            );
        }

        // ==================== STEP 4: Apply status change ====================
        category.setStatus(status);
        foodCategoryRepository.save(category);

        log.info("FoodCategory status updated: publicId={}, newStatus={}",
                publicId, status);

        return ResponseEntity.ok(
                new CommonApiResponse(true, "Food category status updated successfully")
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getFoodCategoryImage(String publicId) {
        
        log.info("Requesting food category image for publicId: {}", publicId);
        
        // ==================== STEP 1: Fetch food category ====================
        var categoryOpt = foodCategoryRepository.findByPublicId(publicId);
        
        if (categoryOpt.isEmpty()) {
            log.warn("Food category not found when requesting image: {}", publicId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        FoodCategory category = categoryOpt.get();
        
        // ==================== STEP 2: Check if category has an image ====================
        String imagePath = category.getImagePath();
        
        if (imagePath == null || imagePath.trim().isEmpty()) {
            log.warn("Food category {} has no image path", publicId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        // ==================== STEP 3: Load and serve the image ====================
        try {
            Resource resource = storageService.loadAsResource(
                imagePath, 
                StorageService.StorageType.FOOD_CATEGORIES
            );
            
            if (resource == null || !resource.exists()) {
                log.warn("Food category image resource not found for category {} path={}", 
                    publicId, imagePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Attempt to determine content type from filename
            String contentType = null;
            try {
                contentType = URLConnection.guessContentTypeFromName(resource.getFilename());
            } catch (Exception ignored) {
                log.debug("Could not guess content type from filename");
            }

            // Fallback to default if content type could not be determined
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.info("Successfully loading food category image for {}: {}", publicId, resource.getFilename());
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"" + resource.getFilename() + "\""
                    )
                    .body(resource);

        } catch (IOException ioe) {
            log.error("Error loading food category image resource for category {}", publicId, ioe);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception ex) {
            log.error("Unexpected error loading image for food category {}", publicId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== Utility Methods ====================

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
