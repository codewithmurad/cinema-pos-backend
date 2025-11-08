package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationInfo {
 
	private int currentPage;
    
	private int pageSize;
    
	private long totalItems;
    
	private int totalPages;
    
	private boolean hasNext;
    
	private boolean hasPrevious;

}