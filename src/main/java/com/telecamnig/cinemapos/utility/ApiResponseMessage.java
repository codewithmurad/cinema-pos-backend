package com.telecamnig.cinemapos.utility;

/**
 * Centralized constants for all API response messages.
 * Keeps messages consistent and maintainable across the project.
 */
public final class ApiResponseMessage {

    private ApiResponseMessage() {
        // prevent instantiation
    }

    // -------------------- Authentication --------------------
    public static final String LOGIN_SUCCESS = "Login successful";
    public static final String INVALID_CREDENTIALS = "Invalid email or password";
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String EMAIL_ALREADY_EXISTS = "User with this email already exists";
    public static final String EMAIL_NOT_FOUND = "No user found with this email";
    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String PASSWORD_CHANGE_SUCCESS = "Password changed successfully";
    public static final String PASSWORD_OLD_INCORRECT = "Old password is incorrect";
    public static final String PASSWORD_MISMATCH = "Passwords do not match";
    public static final String PASSWORD_SAME_AS_OLD = "New password cannot be same as old password";
    public static final String INVALID_USER_STATUS_CODE = "Invalid user status code: ";
    public static final String INVALID_ROLE = "Invalid role: ";
    public static final String FETCH_SUCCESS = "Data fetched successfully";

 // -------------------- JWT / Token --------------------
    public static final String TOKEN_EXPIRED = "Session expired. Please log in again.";
    public static final String TOKEN_INVALID = "Invalid token. Please log in again.";
    public static final String TOKEN_MISSING = "Missing authentication token";
    
    // -------------------- Registration --------------------
    public static final String ADMIN_REGISTER_SUCCESS = "Admin registered successfully";
    public static final String COUNTER_REGISTER_SUCCESS = "Counter staff registered successfully";
    public static final String ROLE_NOT_FOUND = "Required role not found in the system";
    public static final String USER_ALREADY_EXISTS = "User Already Exists";
    
    // -------------------- Validation / Generic Errors --------------------
    public static final String INVALID_INPUT = "Invalid request data";
    public static final String INVALID_DATA_FORMAT = "Invalid date format.";
    public static final String VALIDATION_FAILED = "Validation failed for input fields";
    public static final String UNAUTHORIZED_ACCESS = "You are not authorized to perform this action";
    public static final String ACCESS_DENIED = "You do not have permission to perform this action";
    public static final String FORBIDDEN = "Access forbidden";
    public static final String RESOURCE_NOT_FOUND = "Requested resource not found";
    public static final String INTERNAL_SERVER_ERROR = "An unexpected error occurred. Please try again later.";
    public static final String MISSING_PARAMETER = "Missing Parameter : ";
    public static final String METHOD_NOT_ALLOWED = "HTTP method not allowed on this endpoint.";
    public static final String MALFORMED_JSON_REQUEST = "Malformed JSON request or invalid data format.";
    public static final String PAYLOAD_TOO_LARGE = "File too large. Please upload a smaller file.";
    public static final String NO_DATA_FOUND = "No records found";
    public static final String USER_STATUS_UPDATED = "User status updated successfully";
    public static final String SELF_STATUS_CHANGE_NOT_ALLOWED = "You cannot change your own account status";
    
    // -------------------- File Storage --------------------
    public static final String STORAGE_PATH_NOT_CONFIGURED = "Storage path is not configured";
    public static final String STORAGE_DIRECTORY_CREATION_FAILED = "Failed to create storage directory";
    public static final String FILE_EMPTY = "Uploaded file is empty";
    public static final String FILE_TOO_LARGE = "File size exceeds allowed limit";
    public static final String INVALID_FILE_TYPE = "Only JPEG and PNG images are allowed";
    public static final String FILE_PATH_TRAVERSAL_DETECTED = "Invalid file path detected";
    public static final String FILE_NOT_FOUND = "Requested file not found";
    public static final String FILE_URL_MALFORMED = "File URL is malformed";
    public static final String FILE_DELETE_FAILED = "Failed to delete file";

    // --------------------------- MOVIE MODULE ---------------------------

    public static final String MOVIE_CREATED = "Movie has been added successfully.";
    public static final String MOVIE_ALREADY_EXISTS = "A movie with this title already exists.";
    public static final String INVALID_MOVIE_STATUS = "Invalid movie status code.";
    public static final String INVALID_MOVIE_STATUS_CODE = "Invalid movie status code: ";
    public static final String MOVIE_NOT_FOUND = "Movie not found.";
    public static final String MOVIE_STATUS_UPDATED = "Movie status has been updated successfully.";
    public static final String NO_CHANGE = "No changes detected. Status remains the same.";
    public static final String MOVIE_UPDATED = "Movie details updated successfully.";
    public static final String MOVIE_POSTER_UPDATED = "Movie poster updated successfully.";
    public static final String INVALID_POSTER_FILE_TYPE = "Invalid poster file type. Only JPG, JPEG and PNG are allowed.";
    public static final String MOVIE_POSTER_UPLOAD_FAILED = "Failed to upload poster. Try again.";
    
    // --------------------------- SHOW MODULE ---------------------------
    
    public static final String SHOW_SCHEDULED_SUCCESS = "Show scheduled successfully";
    public static final String SHOW_SCHEDULING_FAILED = "Show scheduling failed";
    public static final String SCREEN_NOT_FOUND = "Screen not found";
    public static final String SCREEN_NOT_ACTIVE = "Screen is not active";
    public static final String MOVIE_NOT_ACTIVE = "Movie is not active";
    public static final String NO_ACTIVE_SEATS = "Screen has no active seats configured";
    public static final String INVALID_SEAT_PRICE = "Seat price must be positive";
    public static final String SHOW_NOT_FOUND = "Show not found";
    public static final String INVALID_SHOW_ID = "Invalid show ID provided";
    public static final String NO_SEATS_FOUND = "No seats found for this show";
    public static final String FETCH_FAILED = "Failed to fetch data";

}
