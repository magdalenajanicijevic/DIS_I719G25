package com.ecommerce.inventoryservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.inventoryservice.dto.CreateInventoryRequest;
import com.ecommerce.inventoryservice.dto.InventoryQuantityRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.UpdateInventoryRequest;
import com.ecommerce.inventoryservice.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Validated
@Tag(name = "Inventory Controller", description = "Operations related to inventory management")
public class InventoryController {

	private final InventoryService inventoryService;

	@PostMapping
	@Operation(summary = "Create inventory")
	public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest request,
			@RequestHeader("X-User-Role") String currentUserRole) {

		InventoryResponse response = inventoryService.createInventory(request, currentUserRole);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get inventory by id")
	public ResponseEntity<InventoryResponse> getInventoryById(@PathVariable Long id,
			@RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(inventoryService.getInventoryById(id, currentUserRole));
	}

	@GetMapping("/product/{productId}")
	@Operation(summary = "Get inventory by product id")
	public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId,
			@RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId, currentUserRole));
	}

	@GetMapping("/internal/product/{productId}")
	@Operation(summary = "Internal - Get inventory by product id")
	public ResponseEntity<InventoryResponse> getInventoryByProductIdInternal(@PathVariable Long productId) {

		return ResponseEntity.ok(inventoryService.getInventoryByProductIdInternal(productId));
	}

	@GetMapping
	@Operation(summary = "Get all inventory")
	public ResponseEntity<List<InventoryResponse>> getAllInventory(
			@RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(inventoryService.getAllInventory(currentUserRole));
	}

	@PostMapping("/internal/reserve")
	public ResponseEntity<Void> reserveInventory(@Valid @RequestBody InventoryQuantityRequest request) {

		inventoryService.reserveInventory(request);

		return ResponseEntity.ok().build();
	}

	@PostMapping("/internal/release")
	public ResponseEntity<Void> releaseInventory(@Valid @RequestBody InventoryQuantityRequest request) {

		inventoryService.releaseInventory(request);

		return ResponseEntity.ok().build();
	}

	@PostMapping("/internal/deduct")
	public ResponseEntity<Void> deductInventory(@Valid @RequestBody InventoryQuantityRequest request) {

		inventoryService.deductInventory(request);

		return ResponseEntity.ok().build();
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update inventory quantity")
	public ResponseEntity<InventoryResponse> updateInventory(@PathVariable Long id,
			@Valid @RequestBody UpdateInventoryRequest request, @RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(inventoryService.updateInventory(id, request, currentUserRole));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete inventory")
	public ResponseEntity<Void> deleteInventory(@PathVariable Long id,
			@RequestHeader("X-User-Role") String currentUserRole) {

		inventoryService.deleteInventory(id, currentUserRole);

		return ResponseEntity.noContent().build();
	}
}