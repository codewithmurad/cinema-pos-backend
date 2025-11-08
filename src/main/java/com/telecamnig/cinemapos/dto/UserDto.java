package com.telecamnig.cinemapos.dto;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public DTO for returning user info to frontend. Contains only non-sensitive
 * fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
	
	private String userId; // public UUID
	
	private String firstName;
	
	private String lastName;
	
	private String emailId;
	
	private String contactNo;
	
	private int status;
	
	private Set<String> roles; // e.g. ["ROLE_ADMIN","ROLE_COUNTER"]
	
	private LocalDateTime createdAt;
	
	private LocalDateTime updatedAt;

}
