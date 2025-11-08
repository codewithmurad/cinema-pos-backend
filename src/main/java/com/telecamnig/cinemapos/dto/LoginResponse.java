package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Specialized response for login endpoint.
 * Extends CommonApiResponse for consistent message/success format.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class LoginResponse extends CommonApiResponse {

    /**
     * JWT token for authenticated user.
     */
    private String token;

    /**
     * Token expiry time (in seconds).
     */
    private Long expiresIn;
    
    /**
     * Complete Detail of logged in user
     */
    private UserDto user;

}
