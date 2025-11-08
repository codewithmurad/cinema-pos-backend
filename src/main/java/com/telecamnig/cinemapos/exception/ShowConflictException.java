package com.telecamnig.cinemapos.exception;

/**
 * Exception thrown when there's a scheduling conflict for a show.
 * This occurs when trying to schedule a show that overlaps with an existing show
 * on the same screen, violating the 30-minute buffer rule.
 * 
 * This exception results in HTTP 409 CONFLICT status.
 */
public class ShowConflictException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new ShowConflictException with the specified detail message.
     *
     * @param message the detail message explaining the conflict
     */
    public ShowConflictException(String message) {
        super(message);
    }

    /**
     * Constructs a new ShowConflictException with the specified detail message and cause.
     *
     * @param message the detail message explaining the conflict
     * @param cause the cause of the conflict
     */
    public ShowConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ShowConflictException with a formatted message.
     *
     * @param moviePublicId the public ID of the conflicting movie
     * @param startAt the start time of the conflicting show
     * @param endAt the end time of the conflicting show
     */
    public ShowConflictException(String moviePublicId, java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
        super(String.format(
            "Time conflict with existing show '%s' scheduled from %s to %s", 
            moviePublicId, startAt, endAt
        ));
    }

}