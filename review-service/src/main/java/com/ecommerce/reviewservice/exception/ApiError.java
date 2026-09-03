package com.ecommerce.reviewservice.exception;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ApiError {

	LocalDateTime timestamp;

	int status;

	String error;

	String message;

	String path;

}
