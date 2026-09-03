package com.ecommerce.orderservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class InventoryQuantityRequest {

	private Long productId;
	private Integer quantity;
}
