package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * WebSocket event for batch seat updates.
 * 
 * Used when multiple seats change state simultaneously (e.g., sofa group booking,
 * bulk seat release, or initial seat map load).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchSeatStateEvent {
    
    private String showPublicId;
    
    private LocalDateTime timestamp;
    
    private java.util.List<SeatStateEvent> seatUpdates;

}
