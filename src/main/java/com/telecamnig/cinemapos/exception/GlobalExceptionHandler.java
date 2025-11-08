package com.telecamnig.cinemapos.exception;

import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.utility.ApiResponseMessage;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.ConstraintViolationException;

/**
 * Handles all global exceptions and returns readable JSON messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------- Validation Errors --------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonApiResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse(ApiResponseMessage.VALIDATION_FAILED);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CommonApiResponse(false, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonApiResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CommonApiResponse(false, ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonApiResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CommonApiResponse(false, ApiResponseMessage.MISSING_PARAMETER + ex.getParameterName()));
    }

    // -------------------- JWT / Security Errors --------------------

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<CommonApiResponse> handleJwtExpired(ExpiredJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new CommonApiResponse(false, ApiResponseMessage.TOKEN_EXPIRED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonApiResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new CommonApiResponse(false, ApiResponseMessage.UNAUTHORIZED_ACCESS));
    }

    // -------------------- HTTP Errors --------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonApiResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new CommonApiResponse(false, ApiResponseMessage.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonApiResponse> handleBadJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CommonApiResponse(false, ApiResponseMessage.MALFORMED_JSON_REQUEST));
    }

    // -------------------- File / Upload / Size Errors --------------------

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<CommonApiResponse> handleMaxSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new CommonApiResponse(false, ApiResponseMessage.PAYLOAD_TOO_LARGE));
    }

    // -------------------- Common Java Exceptions --------------------

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<CommonApiResponse> handleNullPointer(NullPointerException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CommonApiResponse(false, ApiResponseMessage.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<CommonApiResponse> handleDateParse(DateTimeParseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CommonApiResponse(false, ApiResponseMessage.INVALID_DATA_FORMAT));
    }

    // -------------------- Generic Fallback --------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonApiResponse> handleGeneric(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CommonApiResponse(false, ApiResponseMessage.INTERNAL_SERVER_ERROR));
    }
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<CommonApiResponse> handleUserExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CommonApiResponse(false, ex.getMessage() == null ? ApiResponseMessage.USER_ALREADY_EXISTS : ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<CommonApiResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new CommonApiResponse(false, ex.getMessage() == null ? ApiResponseMessage.INVALID_CREDENTIALS : ex.getMessage()));
    }
    
 // Add these methods to your existing GlobalExceptionHandler class

    @ExceptionHandler(ShowConflictException.class)
    public ResponseEntity<CommonApiResponse> handleShowConflict(ShowConflictException ex) {

    	return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CommonApiResponse(false, ex.getMessage()));
    }

    @ExceptionHandler(InvalidSeatTypeException.class)
    public ResponseEntity<CommonApiResponse> handleInvalidSeatType(InvalidSeatTypeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CommonApiResponse(false, ex.getMessage()));
    }

}
