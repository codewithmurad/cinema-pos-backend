package com.telecamnig.cinemapos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatPosition {

	private int x;
	
	private int y;
	
	private int width;
	
	private int height;

	public SeatPosition(int x, int y) {
		this(x, y, 40, 40); // Default seat size
	}

}