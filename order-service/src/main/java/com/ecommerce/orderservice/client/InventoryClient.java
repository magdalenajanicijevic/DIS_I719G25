package com.ecommerce.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ecommerce.orderservice.config.FeignConfig;
import com.ecommerce.orderservice.dto.InventoryQuantityRequest;

@FeignClient(name = "inventory-service", configuration = FeignConfig.class)
public interface InventoryClient {

	@GetMapping("/api/inventory/internal/product/{productId}")
	InventoryClientResponse getInventoryByProductId(@PathVariable Long productId);

	@PostMapping("/api/inventory/internal/reserve")
	void reserveInventory(@RequestBody InventoryQuantityRequest request);
	
	@PostMapping("/api/inventory/internal/release")
	void releaseInventory(@RequestBody InventoryQuantityRequest request);
	
	@PostMapping("/api/inventory/internal/deduct")
	void deductInventory(@RequestBody InventoryQuantityRequest request);

}
