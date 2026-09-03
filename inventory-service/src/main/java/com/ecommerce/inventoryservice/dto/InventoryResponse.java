package com.ecommerce.inventoryservice.dto;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {

	private Long id;

	private Long productId;

	private Integer quantity;

	private Integer reservedQuantity;

	private Integer availableQuantity;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
