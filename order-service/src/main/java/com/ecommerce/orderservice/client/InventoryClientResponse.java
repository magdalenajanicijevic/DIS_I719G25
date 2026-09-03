package com.ecommerce.orderservice.client;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryClientResponse {

	private Long id;

	private Long productId;

	private Integer quantity;

	private Integer reservedQuantity;

	private Integer availableQuantity;

}