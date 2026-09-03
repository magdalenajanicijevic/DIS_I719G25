package com.ecommerce.orderservice.exception;

public class InventoryServiceUnavailableException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InventoryServiceUnavailableException(String message) {
		super(message);
	}
}
