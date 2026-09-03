package com.ecommerce.orderservice.client;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserClientResponse {

	private Long id;
	
	private String firstName;
	
	private String lastName;
	
	private String email;
	
	private String role;

}
