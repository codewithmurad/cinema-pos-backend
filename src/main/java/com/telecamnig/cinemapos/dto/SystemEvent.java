package com.telecamnig.cinemapos.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket event for connection status.
 * 
 * Used to notify clients about system status, maintenance windows, or errors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemEvent {

	private String type; // INFO, WARNING, ERROR, MAINTENANCE

	private String message;

	private LocalDateTime timestamp;

	private String action; // Optional: RECONNECT, REFRESH, etc.

}