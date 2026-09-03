package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryQuantityRequest {

	@NotNull
	private Long productId;

	@NotNull
	@Min(1)
	private Integer quantity;
}
