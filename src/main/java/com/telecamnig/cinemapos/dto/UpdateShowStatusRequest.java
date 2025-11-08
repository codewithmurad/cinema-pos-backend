package com.telecamnig.cinemapos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for updating show status.
 * Used for manual status changes and show cancellations.
 */
@Data
public class UpdateShowStatusRequest {
    
    @NotNull(message = "Status is required")
    private Integer status;
    
    private String reason; // Optional reason for status change (especially for cancellations)

}