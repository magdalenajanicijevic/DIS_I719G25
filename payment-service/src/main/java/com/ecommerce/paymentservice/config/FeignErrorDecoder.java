package com.ecommerce.paymentservice.config;

import com.ecommerce.paymentservice.exception.ResourceNotFoundException;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder {

	@Override
	public Exception decode(String methodKey, Response response) {

		return switch (response.status()) {

		case 400 -> new IllegalArgumentException("Bad request while calling remote service.");

		case 404 -> new ResourceNotFoundException("Requested resource was not found.");

		case 409 -> new IllegalStateException("Conflict while calling remote service.");

		case 500 -> new RuntimeException("Remote service error.");

		default -> new RuntimeException("Unexpected error while calling remote service.");
		};
	}

}
