package com.ecommerce.inventoryservice.exception;

public class InventoryAlreadyExistsException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InventoryAlreadyExistsException(String message) {
		super(message);
	}

}
