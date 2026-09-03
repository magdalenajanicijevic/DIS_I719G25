package com.ecommerce.orderservice.exception;

public class InactiveProductException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InactiveProductException(String message) {
		super(message);
	}
}
