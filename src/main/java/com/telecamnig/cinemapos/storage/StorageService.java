package com.telecamnig.cinemapos.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    enum StorageType {
        POSTERS,
        FOOD_CATEGORIES,
        FOODS
    }
    
    /**
     * Store the provided multipart file in the specified storage type
     * Returns the relative file name (e.g. 2025/10/23/uuid.jpg).
     */
    String store(MultipartFile file, StorageType type) throws IOException;

    /**
     * Load a stored file as a Spring Resource from specified storage type
     */
    Resource loadAsResource(String filename, StorageType type) throws IOException;

    /**
     * Delete a stored file from specified storage type
     * Returns true if file deleted.
     */
    boolean delete(String filename, StorageType type) throws IOException;
    
    /**
     * Get the full path for a filename in specified storage type
     */
    String getFullPath(String filename, StorageType type);
}