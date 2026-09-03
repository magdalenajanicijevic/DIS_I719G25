package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInventoryRequest {

	@NotNull(message = "Product id is required.")
	private Long productId;

	@NotNull(message = "Quantity is required.")
	@Min(value = 0, message = "Quantity cannot be negative.")
	private Integer quantity;

}
