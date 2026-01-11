// DashboardResponse.java (extends CommonApiResponse)
package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse extends CommonApiResponse {
    
    private DashboardResponseDTO data;
    
}