package com.telecamnig.cinemapos.dto;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Response wrapper for lists of shows with pagination support.
 * Used for show listing pages and dashboards.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ShowsListResponse extends CommonApiResponse {
	
    private List<ShowListItemDto> shows;
    
    // Pagination metadata
    private Integer page;
    
    private Integer size;
    
    private Long totalElements;
    
    private Integer totalPages;
    
    // Summary statistics
    private ShowsSummaryDto summary;

}