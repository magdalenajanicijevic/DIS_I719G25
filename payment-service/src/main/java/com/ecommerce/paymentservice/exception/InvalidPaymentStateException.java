package com.ecommerce.paymentservice.exception;

public class InvalidPaymentStateException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidPaymentStateException(String message) {
        super(message);
    }

}
