package com.ecommerce.userservice.exception;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {

	private LocalDateTime timestamp;
	
	private int status;
	
	private String error;
	
	private String message;

}
