package com.telecamnig.cinemapos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRegisterRequest {

	@NotBlank(message = "First name is required")
	@Size(max = 100)
	private String firstName;

	@Size(max = 100)
	private String lastName;

	@NotBlank(message = "Email is required")
	@Email(message = "Must be a valid email")
	@Size(max = 255)
	private String email;

	@NotBlank(message = "Contact number is required")
	@Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format")
	private String contact;

	@NotBlank(message = "Password is required")
	@Size(min = 6, max = 128)
	private String password;

}
