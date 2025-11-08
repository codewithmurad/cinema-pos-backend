package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Minimal API response wrapper.
 * UI decides success/failure by HTTP status; body contains human message and success flag.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CommonApiResponse {
    /**
     * true if operation succeeded (for convenience), false otherwise.
     * UI will primarily rely on HTTP status codes.
     */
    private boolean success;

    /**
     * Human-friendly message to display in UI (toast).
     */
    private String message;
    
}
