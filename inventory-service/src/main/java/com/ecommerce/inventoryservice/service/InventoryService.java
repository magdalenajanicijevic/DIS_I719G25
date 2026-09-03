package com.ecommerce.inventoryservice.service;

import java.util.List;

import com.ecommerce.inventoryservice.dto.CreateInventoryRequest;
import com.ecommerce.inventoryservice.dto.InventoryQuantityRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.UpdateInventoryRequest;

public interface InventoryService {

	InventoryResponse createInventory(CreateInventoryRequest request, String currentUserRole);

	InventoryResponse getInventoryById(Long id, String currentUserRole);

	InventoryResponse getInventoryByProductId(Long productId, String currentUserRole);

	List<InventoryResponse> getAllInventory(String currentUserRole);

	InventoryResponse getInventoryByProductIdInternal(Long productId);

	InventoryResponse updateInventory(Long id, UpdateInventoryRequest request, String currentUserRole);

	void deleteInventory(Long id, String currentUserRole);

	void reserveInventory(InventoryQuantityRequest request);

	void releaseInventory(InventoryQuantityRequest request);

	void deductInventory(InventoryQuantityRequest request);
}