package com.telecamnig.cinemapos.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.telecamnig.cinemapos.utility.ApiResponseMessage;

@Service
public class LocalStorageService implements StorageService {

    private final Map<StorageType, Path> storageLocations;
    private final long maxFileSizeBytes;

    // preferred ext mapping for content types (lower-case keys)
    private static final Map<String, String> CONTENT_TYPE_TO_EXT = Map.of(
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            "image/jpg", ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png");

    private static final Set<String> ALLOWED_TYPES = CONTENT_TYPE_TO_EXT.keySet();

    public LocalStorageService(
            @Value("${app.storage.posters}") String postersPath,
            @Value("${app.storage.food-categories}") String foodCategoriesPath,
            @Value("${app.storage.foods}") String foodsPath,
            @Value("${app.storage.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.storageLocations = new EnumMap<>(StorageType.class);
        
        // Initialize all storage locations
        storageLocations.put(StorageType.POSTERS, initializeLocation(postersPath, "posters"));
        storageLocations.put(StorageType.FOOD_CATEGORIES, initializeLocation(foodCategoriesPath, "food-categories"));
        storageLocations.put(StorageType.FOODS, initializeLocation(foodsPath, "foods"));
    }
    
    private Path initializeLocation(String path, String typeName) {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException(
                String.format(ApiResponseMessage.STORAGE_PATH_NOT_CONFIGURED, typeName));
        }
        
        Path location = Paths.get(path).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(location);
            System.out.println("Storage initialized for " + typeName + ": " + location);
            return location;
        } catch (IOException e) {
            throw new IllegalStateException(
                String.format(ApiResponseMessage.STORAGE_DIRECTORY_CREATION_FAILED + ": %s", path), e);
        }
    }

    @Override
    public String store(MultipartFile file, StorageType type) throws IOException {
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(ApiResponseMessage.FILE_EMPTY);
        }

        Path rootLocation = getLocationForType(type);

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    ApiResponseMessage.FILE_TOO_LARGE + " (Limit: " + (maxFileSizeBytes / (1024 * 1024)) + " MB)");
        }

        String contentType = file.getContentType();
        
        if (contentType == null) {
            throw new IllegalArgumentException(ApiResponseMessage.INVALID_FILE_TYPE);
        }
        
        String contentTypeNormalized = contentType.toLowerCase();

        if (!ALLOWED_TYPES.contains(contentTypeNormalized)) {
            throw new IllegalArgumentException(ApiResponseMessage.INVALID_FILE_TYPE);
        }

        // Folder by date: /YYYY/MM/DD/
        LocalDate date = LocalDate.now();
        
        Path dateDir = rootLocation.resolve(Paths.get(
            String.valueOf(date.getYear()),
            String.format("%02d", date.getMonthValue()),
            String.format("%02d", date.getDayOfMonth())
        ));
        
        Files.createDirectories(dateDir);

        // Clean filename & determine extension
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        
        String ext = "";
        
        int dot = originalName.lastIndexOf('.');
        
        if (dot >= 0) {
            ext = originalName.substring(dot).toLowerCase();
            // Normalize .jpeg to .jpg
            if (ext.equals(".jpeg")) {
                ext = ".jpg";
            }
            // Accept only known extensions; else fallback to mapping by content type
            if (!ext.equals(".jpg") && !ext.equals(".png")) {
                ext = CONTENT_TYPE_TO_EXT.getOrDefault(contentTypeNormalized, "");
            }
        } else {
            // derive from content type mapping
            ext = CONTENT_TYPE_TO_EXT.getOrDefault(contentTypeNormalized, "");
        }

        if (ext.isBlank()) {
            throw new IllegalArgumentException(ApiResponseMessage.INVALID_FILE_TYPE);
        }

        String uuidFileName = UUID.randomUUID().toString() + ext;
        
        Path destination = dateDir.resolve(uuidFileName).normalize();

        // Prevent directory traversal
        if (!destination.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
            throw new SecurityException(ApiResponseMessage.FILE_PATH_TRAVERSAL_DETECTED);
        }

        // Store file
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        // Return relative path for DB storage (use forward slashes)
        return rootLocation.relativize(destination).toString().replace("\\", "/");
    }

    @Override
    public Resource loadAsResource(String filename, StorageType type) throws IOException {
        
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(ApiResponseMessage.INVALID_INPUT);
        }

        Path rootLocation = getLocationForType(type);
        Path file = rootLocation.resolve(filename).normalize();
        
        if (!file.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
            throw new SecurityException(ApiResponseMessage.FILE_PATH_TRAVERSAL_DETECTED);
        }

        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw new NoSuchFileException(ApiResponseMessage.FILE_NOT_FOUND + ": " + filename);
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new NoSuchFileException(ApiResponseMessage.FILE_NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            throw new IOException(ApiResponseMessage.FILE_URL_MALFORMED, e);
        }
    }

    @Override
    public boolean delete(String filename, StorageType type) throws IOException {
        
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(ApiResponseMessage.INVALID_INPUT);
        }

        Path rootLocation = getLocationForType(type);
        Path file = rootLocation.resolve(filename).normalize();
        
        if (!file.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
            throw new SecurityException(ApiResponseMessage.FILE_PATH_TRAVERSAL_DETECTED);
        }

        try {
            return Files.deleteIfExists(file);
        } catch (DirectoryNotEmptyException e) {
            throw new IOException(ApiResponseMessage.FILE_DELETE_FAILED + " - Directory not empty", e);
        } catch (IOException e) {
            throw new IOException(ApiResponseMessage.FILE_DELETE_FAILED, e);
        }
    }

    @Override
    public String getFullPath(String filename, StorageType type) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        
        Path rootLocation = getLocationForType(type);
        Path file = rootLocation.resolve(filename).normalize();
        return file.toAbsolutePath().toString();
    }
    
    private Path getLocationForType(StorageType type) {
        Path location = storageLocations.get(type);
        if (location == null) {
            throw new IllegalArgumentException("Invalid storage type: " + type);
        }
        return location;
    }
    
    /**
     * Helper method to get the storage path for a specific type
     */
    public String getStoragePath(StorageType type) {
        return getLocationForType(type).toString();
    }
}