package com.telecamnig.cinemapos.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    /**
     * Store the provided multipart file and return the relative file name (e.g. 2025/10/23/uuid.jpg).
     */
    String store(MultipartFile file) throws IOException;

    /**
     * Load a stored file as a Spring Resource by its stored filename (relative path).
     */
    Resource loadAsResource(String filename) throws IOException;

    /**
     * Delete a stored file by filename (relative path). Returns true if file deleted.
     */
    boolean delete(String filename) throws IOException;
    
}
