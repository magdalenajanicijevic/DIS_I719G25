package com.ecommerce.paymentservice.exception;

public class PaymentAlreadyExistsException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PaymentAlreadyExistsException(String message) {
        super(message);
    }

}
