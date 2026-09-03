package com.ecommerce.orderservice.exception;

public class InvalidOrderStateException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidOrderStateException(String message) {
        super(message);
    }

}
