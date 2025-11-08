package com.telecamnig.cinemapos.exception;

public class InvalidSeatTypeException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public InvalidSeatTypeException(String message) {
		super(message);
	}

}