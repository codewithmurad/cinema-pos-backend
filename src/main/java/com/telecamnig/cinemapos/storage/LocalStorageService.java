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

	private final Path rootLocation;

	private final long maxFileSizeBytes;

	// preferred ext mapping for content types (lower-case keys)
	private static final Map<String, String> CONTENT_TYPE_TO_EXT = Map.of(
			MediaType.IMAGE_JPEG_VALUE, ".jpeg",
			"image/jpg",
			".jpg",
			MediaType.IMAGE_PNG_VALUE,
			".png");

    private static final String IMAGE_JPEG = ".jpeg";
    
    private static final String IMAGE_JPG = ".jpg";
    
    private static final String IMAGE_PNG = ".png";
	
	// allowed content types
	private static final Set<String> ALLOWED_TYPES = CONTENT_TYPE_TO_EXT.keySet();

	// default 10MB
	public LocalStorageService(
			@Value("${app.storage.posters}") String postersPath,
			@Value("${app.storage.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
		
		if (postersPath == null || postersPath.isBlank()) {
			throw new IllegalStateException(ApiResponseMessage.STORAGE_PATH_NOT_CONFIGURED);
		}
		
		this.rootLocation = Paths.get(postersPath).toAbsolutePath().normalize();
		
		this.maxFileSizeBytes = maxFileSizeBytes;
		
		try {
			Files.createDirectories(this.rootLocation);
		} catch (IOException e) {
			throw new IllegalStateException(ApiResponseMessage.STORAGE_DIRECTORY_CREATION_FAILED + ": " + postersPath,
					e);
		}
	}

	@Override
	public String store(MultipartFile file) throws IOException {
		
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException(ApiResponseMessage.FILE_EMPTY);
		}

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
		
		Path dateDir = rootLocation.resolve(Paths.get(String.valueOf(date.getYear()),
				String.format("%02d", date.getMonthValue()), String.format("%02d", date.getDayOfMonth())));
		
		Files.createDirectories(dateDir);

		// Clean filename & determine extension
		String originalName = StringUtils.cleanPath(file.getOriginalFilename());
		
		String ext = "";
		
		int dot = originalName.lastIndexOf('.');
		
		if (dot >= 0) {
			ext = originalName.substring(dot).toLowerCase();
			// normalize common variants: .jpeg -> .jpg
			if (IMAGE_JPEG.equals(ext)) {
				ext = IMAGE_JPG;
			}
			// accept only known extensions; else fallback to mapping by content type
			if (!IMAGE_JPG.equals(ext) && !IMAGE_PNG.equals(ext)) {
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

		// Prevent directory traversal - ensure destination is under rootLocation
		if (!destination.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
			throw new SecurityException(ApiResponseMessage.FILE_PATH_TRAVERSAL_DETECTED);
		}

		// Store file
		Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

		// Return relative path for DB storage (use forward slashes)
		return rootLocation.relativize(destination).toString().replace("\\", "/");
	}

	@Override
	public Resource loadAsResource(String filename) throws IOException {
		
		if (filename == null || filename.isBlank()) {
			throw new IllegalArgumentException(ApiResponseMessage.INVALID_INPUT);
		}

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
	public boolean delete(String filename) throws IOException {
		
		if (filename == null || filename.isBlank()) {
			throw new IllegalArgumentException(ApiResponseMessage.INVALID_INPUT);
		}

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
}
