package com.telecamnig.cinemapos.dto;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Data;

/**
 * Response wrapper for user details endpoint. Extends CommonApiResponse so UI
 * always gets success + message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class UserResponse extends CommonApiResponse {

	private UserDto user;

}
