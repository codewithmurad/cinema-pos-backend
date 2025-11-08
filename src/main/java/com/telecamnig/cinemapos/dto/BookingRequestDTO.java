package com.telecamnig.cinemapos.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {
    
    @NotBlank(message = "Show public ID is required")
    private String showPublicId;
    
    @NotEmpty(message = "At least one seat must be selected")
    private List<@NotBlank String> seatPublicIds;
    
    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String customerName;
    
    @NotBlank(message = "Customer phone is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format")
    private String customerPhone;
    
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String customerEmail;
    
    @NotBlank(message = "Payment mode is required")
    private String paymentMode;
    
    @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    private String transactionReference;
    
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

}