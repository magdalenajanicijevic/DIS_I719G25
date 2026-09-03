package com.ecommerce.inventoryservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.inventoryservice.client.ProductClient;
import com.ecommerce.inventoryservice.client.ProductClientResponse;
import com.ecommerce.inventoryservice.dto.CreateInventoryRequest;
import com.ecommerce.inventoryservice.dto.InventoryQuantityRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.UpdateInventoryRequest;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.exception.ForbiddenException;
import com.ecommerce.inventoryservice.exception.InactiveProductException;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InvalidInventoryOperationException;
import com.ecommerce.inventoryservice.exception.InventoryAlreadyExistsException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.repository.InventoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

	private final InventoryRepository inventoryRepository;
	private final ProductClient productClient;

	@Override
	public InventoryResponse createInventory(CreateInventoryRequest request, String currentUserRole) {

		requireAdmin(currentUserRole);

		ProductClientResponse product = productClient.getProductById(request.getProductId());

		if (!product.isActive()) {
			throw new InactiveProductException("Inventory cannot be created for an inactive product.");
		}

		if (inventoryRepository.existsByProductId(request.getProductId())) {
			throw new InventoryAlreadyExistsException(
					"Inventory already exists for product id " + request.getProductId());
		}

		Inventory inventory = Inventory.builder().productId(request.getProductId()).quantity(request.getQuantity())
				.reservedQuantity(0).build();

		Inventory savedInventory = inventoryRepository.save(inventory);

		return mapToResponse(savedInventory);
	}

	@Override
	public InventoryResponse getInventoryById(Long id, String currentUserRole) {

		requireAdmin(currentUserRole);

		return mapToResponse(findInventoryById(id));
	}

	@Override
	public InventoryResponse getInventoryByProductId(Long productId, String currentUserRole) {

		requireAdmin(currentUserRole);

		Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow(
				() -> new InventoryNotFoundException("Inventory for product id " + productId + " not found."));

		return mapToResponse(inventory);
	}

	@Override
	public List<InventoryResponse> getAllInventory(String currentUserRole) {

		requireAdmin(currentUserRole);

		return inventoryRepository.findAll().stream().map(this::mapToResponse).toList();
	}

	@Override
	public InventoryResponse getInventoryByProductIdInternal(Long productId) {

		Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow(
				() -> new InventoryNotFoundException("Inventory for product id " + productId + " not found."));

		return mapToResponse(inventory);
	}

	@Override
	public InventoryResponse updateInventory(Long id, UpdateInventoryRequest request, String currentUserRole) {

		requireAdmin(currentUserRole);

		Inventory inventory = findInventoryById(id);

		inventory.setQuantity(request.getQuantity());

		Inventory updatedInventory = inventoryRepository.save(inventory);

		return mapToResponse(updatedInventory);
	}

	@Override
	public void deleteInventory(Long id, String currentUserRole) {

		requireAdmin(currentUserRole);

		Inventory inventory = findInventoryById(id);

		inventoryRepository.delete(inventory);
	}

	@Override
	public void reserveInventory(InventoryQuantityRequest request) {

		Inventory inventory = findInventoryByProductId(request.getProductId());

		int available = inventory.getQuantity() - inventory.getReservedQuantity();

		if (available < request.getQuantity()) {
			throw new InsufficientStockException("Insufficient stock available.");
		}

		inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());

		inventoryRepository.save(inventory);
	}

	@Override
	public void releaseInventory(InventoryQuantityRequest request) {

		Inventory inventory = findInventoryByProductId(request.getProductId());

		if (inventory.getReservedQuantity() < request.getQuantity()) {

			throw new InvalidInventoryOperationException(
					"Cannot release more reserved items " + "than currently reserved.");
		}

		inventory.setReservedQuantity(inventory.getReservedQuantity() - request.getQuantity());

		inventoryRepository.save(inventory);
	}

	@Override
	public void deductInventory(InventoryQuantityRequest request) {

		Inventory inventory = findInventoryByProductId(request.getProductId());

		if (inventory.getReservedQuantity() < request.getQuantity()) {

			throw new InvalidInventoryOperationException(
					"Cannot deduct more reserved items " + "than currently reserved.");
		}

		inventory.setQuantity(inventory.getQuantity() - request.getQuantity());

		inventory.setReservedQuantity(inventory.getReservedQuantity() - request.getQuantity());

		inventoryRepository.save(inventory);
	}

	private Inventory findInventoryById(Long id) {

		return inventoryRepository.findById(id)
				.orElseThrow(() -> new InventoryNotFoundException("Inventory with id " + id + " not found."));
	}

	private Inventory findInventoryByProductId(Long productId) {

		return inventoryRepository.findByProductId(productId).orElseThrow(
				() -> new InventoryNotFoundException("Inventory for product id " + productId + " not found."));
	}

	private void requireAdmin(String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can manage inventory.");
		}
	}

	private InventoryResponse mapToResponse(Inventory inventory) {

		return InventoryResponse.builder().id(inventory.getId()).productId(inventory.getProductId())
				.quantity(inventory.getQuantity()).reservedQuantity(inventory.getReservedQuantity())
				.availableQuantity(inventory.getQuantity() - inventory.getReservedQuantity())
				.createdAt(inventory.getCreatedAt()).updatedAt(inventory.getUpdatedAt()).build();
	}
}