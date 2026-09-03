package com.ecommerce.orderservice.exception;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiError {

	private LocalDateTime timestamp;

	private int status;

	private String error;

	private String message;

}
