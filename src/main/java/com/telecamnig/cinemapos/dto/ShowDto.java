package com.telecamnig.cinemapos.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ShowDto {
	
    private String publicId;
    
    private String moviePublicId;
    
    private String screenPublicId;
    
    private LocalDateTime startAt;
    
    private LocalDateTime endAt;
    
    private Integer status;
    
    private LocalDateTime createdAt;

}