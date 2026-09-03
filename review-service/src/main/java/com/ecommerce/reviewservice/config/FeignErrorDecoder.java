package com.ecommerce.reviewservice.config;

import com.ecommerce.reviewservice.exception.BadRequestException;
import com.ecommerce.reviewservice.exception.ConflictException;
import com.ecommerce.reviewservice.exception.ResourceNotFoundException;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder {

	private final ErrorDecoder defaultDecoder = new Default();

	@Override
	public Exception decode(String methodKey, Response response) {

		switch (response.status()) {

		case 400:
			return new BadRequestException("Bad request to remote service.");

		case 404:
			return new ResourceNotFoundException("Requested resource was not found.");

		case 409:
			return new ConflictException("Conflict while calling remote service.");

		case 500:
			return new RuntimeException("Remote service encountered an internal error.");

		default:
			return defaultDecoder.decode(methodKey, response);
		}
	}

}