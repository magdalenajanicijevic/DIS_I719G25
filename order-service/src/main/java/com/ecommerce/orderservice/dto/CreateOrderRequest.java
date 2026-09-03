package com.ecommerce.orderservice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

	@NotNull(message = "User id is required.")
	private Long userId;

	@Valid
	@NotEmpty(message = "Order must contain at least one item.")
	private List<OrderItemRequest> items;

}