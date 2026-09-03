package com.ecommerce.orderservice.config;

import com.ecommerce.orderservice.exception.ResourceNotFoundException;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder {

	private final ErrorDecoder defaultDecoder = new Default();

	@Override
	public Exception decode(String methodKey, Response response) {

		switch (response.status()) {

		case 400:
			return new IllegalArgumentException("Bad request to remote service.");

		case 404:
			return new ResourceNotFoundException("The requested user, product, or inventory resource was not found.");
		case 409:
			return new IllegalStateException("Conflict while calling remote service.");

		case 500:
			return new RuntimeException("Remote service encountered an internal error.");

		default:
			return defaultDecoder.decode(methodKey, response);
		}
	}
}
