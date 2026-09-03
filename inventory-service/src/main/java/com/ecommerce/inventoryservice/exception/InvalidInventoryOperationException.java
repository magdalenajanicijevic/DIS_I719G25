package com.ecommerce.inventoryservice.exception;

public class InvalidInventoryOperationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidInventoryOperationException(String message) {
		super(message);
	}
}
