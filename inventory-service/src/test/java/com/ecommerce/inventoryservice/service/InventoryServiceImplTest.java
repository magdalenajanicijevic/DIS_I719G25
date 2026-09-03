package com.ecommerce.inventoryservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

	@Mock
	private InventoryRepository inventoryRepository;

	@Mock
	private ProductClient productClient;

	@InjectMocks
	private InventoryServiceImpl inventoryService;

	private Inventory inventory;
	private CreateInventoryRequest createRequest;
	private UpdateInventoryRequest updateRequest;
	private InventoryQuantityRequest quantityRequest;

	@BeforeEach
	void setUp() {

		inventory = Inventory.builder().id(1L).productId(100L).quantity(50).reservedQuantity(10)
				.createdAt(LocalDateTime.of(2026, 1, 1, 10, 0)).updatedAt(LocalDateTime.of(2026, 1, 1, 10, 0)).build();

		createRequest = CreateInventoryRequest.builder().productId(100L).quantity(50).build();

		updateRequest = UpdateInventoryRequest.builder().quantity(80).build();

		quantityRequest = InventoryQuantityRequest.builder().productId(100L).quantity(10).build();
	}

	@Test
	void createInventory_shouldCreateInventorySuccessfully() {

		ProductClientResponse product = new ProductClientResponse(100L, "Laptop", true);

		when(productClient.getProductById(100L)).thenReturn(product);
		when(inventoryRepository.existsByProductId(100L)).thenReturn(false);
		when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

		InventoryResponse response = inventoryService.createInventory(createRequest, "ADMIN");

		assertNotNull(response);
		assertEquals(inventory.getId(), response.getId());
		assertEquals(inventory.getProductId(), response.getProductId());
		assertEquals(inventory.getQuantity(), response.getQuantity());
		assertEquals(inventory.getReservedQuantity(), response.getReservedQuantity());
		assertEquals(40, response.getAvailableQuantity());

		verify(productClient).getProductById(100L);
		verify(inventoryRepository).existsByProductId(100L);
		verify(inventoryRepository).save(any(Inventory.class));
	}

	@Test
	void createInventory_shouldRejectCustomer() {

		assertThrows(ForbiddenException.class, () -> inventoryService.createInventory(createRequest, "CUSTOMER"));

		verify(productClient, never()).getProductById(any());
		verify(inventoryRepository, never()).existsByProductId(any());
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}

	@Test
	void createInventory_shouldSaveCorrectInventoryData() {

		ProductClientResponse product = new ProductClientResponse(100L, "Laptop", true);

		when(productClient.getProductById(100L)).thenReturn(product);
		when(inventoryRepository.existsByProductId(100L)).thenReturn(false);
		when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		inventoryService.createInventory(createRequest, "ADMIN");

		ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);

		verify(inventoryRepository).save(inventoryCaptor.capture());

		Inventory savedInventory = inventoryCaptor.getValue();

		assertEquals(100L, savedInventory.getProductId());
		assertEquals(50, savedInventory.getQuantity());
		assertEquals(0, savedInventory.getReservedQuantity());
	}

	@Test
	void createInventory_shouldThrowException_whenProductIsInactive() {

		ProductClientResponse product = new ProductClientResponse(100L, "Laptop", false);

		when(productClient.getProductById(100L)).thenReturn(product);

		InactiveProductException exception = assertThrows(InactiveProductException.class,
				() -> inventoryService.createInventory(createRequest, "ADMIN"));

		assertEquals("Inventory cannot be created for an inactive product.", exception.getMessage());

		verify(productClient).getProductById(100L);
		verify(inventoryRepository, never()).existsByProductId(any());
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}

	@Test
	void createInventory_shouldThrowException_whenInventoryAlreadyExists() {

		ProductClientResponse product = new ProductClientResponse(100L, "Laptop", true);

		when(productClient.getProductById(100L)).thenReturn(product);
		when(inventoryRepository.existsByProductId(100L)).thenReturn(true);

		InventoryAlreadyExistsException exception = assertThrows(InventoryAlreadyExistsException.class,
				() -> inventoryService.createInventory(createRequest, "ADMIN"));

		assertEquals("Inventory already exists for product id 100", exception.getMessage());

		verify(productClient).getProductById(100L);
		verify(inventoryRepository).existsByProductId(100L);
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}

	@Test
	void getInventoryById_shouldReturnInventorySuccessfully() {

		when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

		InventoryResponse response = inventoryService.getInventoryById(1L, "ADMIN");

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(100L, response.getProductId());
		assertEquals(50, response.getQuantity());
		assertEquals(10, response.getReservedQuantity());
		assertEquals(40, response.getAvailableQuantity());
		assertEquals(inventory.getCreatedAt(), response.getCreatedAt());
		assertEquals(inventory.getUpdatedAt(), response.getUpdatedAt());

		verify(inventoryRepository).findById(1L);
	}

	@Test
	void getInventoryById_shouldRejectCustomer() {

		assertThrows(ForbiddenException.class, () -> inventoryService.getInventoryById(1L, "CUSTOMER"));

		verify(inventoryRepository, never()).findById(any());
	}

	@Test
	void getInventoryById_shouldThrowException_whenInventoryNotFound() {

		when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());

		InventoryNotFoundException exception = assertThrows(InventoryNotFoundException.class,
				() -> inventoryService.getInventoryById(1L, "ADMIN"));

		assertEquals("Inventory with id 1 not found.", exception.getMessage());

		verify(inventoryRepository).findById(1L);
	}

	@Test
	void getInventoryByProductId_shouldReturnInventorySuccessfully() {

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

		InventoryResponse response = inventoryService.getInventoryByProductId(100L, "ADMIN");

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(100L, response.getProductId());
		assertEquals(50, response.getQuantity());
		assertEquals(10, response.getReservedQuantity());
		assertEquals(40, response.getAvailableQuantity());

		verify(inventoryRepository).findByProductId(100L);
	}

	@Test
	void getInventoryByProductId_shouldRejectCustomer() {

		assertThrows(ForbiddenException.class, () -> inventoryService.getInventoryByProductId(100L, "CUSTOMER"));

		verify(inventoryRepository, never()).findByProductId(any());
	}

	@Test
	void getInventoryByProductId_shouldThrowException_whenInventoryNotFound() {

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.empty());

		InventoryNotFoundException exception = assertThrows(InventoryNotFoundException.class,
				() -> inventoryService.getInventoryByProductId(100L, "ADMIN"));

		assertEquals("Inventory for product id 100 not found.", exception.getMessage());

		verify(inventoryRepository).findByProductId(100L);
	}

	@Test
	void getAllInventory_shouldReturnAllInventory() {

		Inventory secondInventory = Inventory.builder().id(2L).productId(200L).quantity(30).reservedQuantity(5).build();

		when(inventoryRepository.findAll()).thenReturn(List.of(inventory, secondInventory));

		List<InventoryResponse> response = inventoryService.getAllInventory("ADMIN");

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals(1L, response.get(0).getId());
		assertEquals(100L, response.get(0).getProductId());
		assertEquals(40, response.get(0).getAvailableQuantity());

		assertEquals(2L, response.get(1).getId());
		assertEquals(200L, response.get(1).getProductId());
		assertEquals(25, response.get(1).getAvailableQuantity());

		verify(inventoryRepository).findAll();
	}

	@Test
	void getAllInventory_shouldRejectCustomer() {

		assertThrows(ForbiddenException.class, () -> inventoryService.getAllInventory("CUSTOMER"));

		verify(inventoryRepository, never()).findAll();
	}

	@Test
	void getAllInventory_shouldReturnEmptyList_whenNoInventoryExists() {

		when(inventoryRepository.findAll()).thenReturn(List.of());

		List<InventoryResponse> response = inventoryService.getAllInventory("ADMIN");

		assertNotNull(response);
		assertEquals(0, response.size());

		verify(inventoryRepository).findAll();
	}

	@Test
	void getInventoryByProductIdInternal_shouldReturnInventorySuccessfully() {

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

		InventoryResponse response = inventoryService.getInventoryByProductIdInternal(100L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(100L, response.getProductId());
		assertEquals(50, response.getQuantity());
		assertEquals(10, response.getReservedQuantity());
		assertEquals(40, response.getAvailableQuantity());

		verify(inventoryRepository).findByProductId(100L);
	}

	@Test
	void getInventoryByProductIdInternal_shouldThrowException_whenInventoryNotFound() {

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.empty());

		InventoryNotFoundException exception = assertThrows(InventoryNotFoundException.class,
				() -> inventoryService.getInventoryByProductIdInternal(100L));

		assertEquals("Inventory for product id 100 not found.", exception.getMessage());
	}

	@Test
	void updateInventory_shouldUpdateQuantitySuccessfully() {

		when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

		when(inventoryRepository.save(inventory)).thenReturn(inventory);

		InventoryResponse response = inventoryService.updateInventory(1L, updateRequest, "ADMIN");

		assertNotNull(response);
		assertEquals(80, inventory.getQuantity());
		assertEquals(10, inventory.getReservedQuantity());
		assertEquals(70, response.getAvailableQuantity());

		verify(inventoryRepository).findById(1L);
		verify(inventoryRepository).save(inventory);
	}

	@Test
	void updateInventory_shouldRejectCustomer() {

		assertThrows(ForbiddenException.class, () -> inventoryService.updateInventory(1L, updateRequest, "CUSTOMER"));

		verify(inventoryRepository, never()).findById(any());
		verify(inventoryRepository, never()).save(any());
	}

	@Test
	void updateInventory_shouldThrowException_whenInventoryNotFound() {

		when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());

		InventoryNotFoundException exception = assertThrows(InventoryNotFoundException.class,
				() -> inventoryService.updateInventory(1L, updateRequest, "ADMIN"));

		assertEquals("Inventory with id 1 not found.", exception.getMessage());

		verify(inventoryRepository).findById(1L);
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}

	@Test
	void deleteInventory_shouldDeleteInventorySuccessfully() {

		when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

		inventoryService.deleteInventory(1L, "ADMIN");

		verify(inventoryRepository).findById(1L);
		verify(inventoryRepository).delete(inventory);
	}

	@Test
	void deleteInventory_shouldRejectCustomer() {

		assertThrows(ForbiddenException.class, () -> inventoryService.deleteInventory(1L, "CUSTOMER"));

		verify(inventoryRepository, never()).findById(any());
		verify(inventoryRepository, never()).delete(any());
	}

	@Test
	void deleteInventory_shouldThrowException_whenInventoryNotFound() {

		when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());

		InventoryNotFoundException exception = assertThrows(InventoryNotFoundException.class,
				() -> inventoryService.deleteInventory(1L, "ADMIN"));

		assertEquals("Inventory with id 1 not found.", exception.getMessage());

		verify(inventoryRepository).findById(1L);
		verify(inventoryRepository, never()).delete(any(Inventory.class));
	}

	@Test
	void reserveInventory_shouldReserveStockSuccessfully() {

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

		inventoryService.reserveInventory(quantityRequest);

		assertEquals(20, inventory.getReservedQuantity());

		verify(inventoryRepository).findByProductId(100L);
		verify(inventoryRepository).save(inventory);
	}

	@Test
	void reserveInventory_shouldAllowReservationOfExactlyAvailableQuantity() {

		Inventory exactInventory = Inventory.builder().id(1L).productId(100L).quantity(50).reservedQuantity(10).build();

		InventoryQuantityRequest exactRequest = InventoryQuantityRequest.builder().productId(100L).quantity(40).build();

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(exactInventory));

		inventoryService.reserveInventory(exactRequest);

		assertEquals(50, exactInventory.getReservedQuantity());

		verify(inventoryRepository).save(exactInventory);
	}

	@Test
	void reserveInventory_shouldThrowException_whenStockIsInsufficient() {

		InventoryQuantityRequest request = InventoryQuantityRequest.builder().productId(100L).quantity(41).build();

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

		InsufficientStockException exception = assertThrows(InsufficientStockException.class,
				() -> inventoryService.reserveInventory(request));

		assertEquals("Insufficient stock available.", exception.getMessage());

		assertEquals(10, inventory.getReservedQuantity());

		verify(inventoryRepository).findByProductId(100L);
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}

	@Test
	void reserveInventory_shouldThrowException_whenInventoryNotFound() {

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.empty());

		InventoryNotFoundException exception = assertThrows(InventoryNotFoundException.class,
				() -> inventoryService.reserveInventory(quantityRequest));

		assertEquals("Inventory for product id 100 not found.", exception.getMessage());

		verify(inventoryRepository).findByProductId(100L);
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}

	@Test
	void releaseInventory_shouldReleaseReservedStockSuccessfully() {

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

		inventoryService.releaseInventory(quantityRequest);

		assertEquals(0, inventory.getReservedQuantity());

		verify(inventoryRepository).findByProductId(100L);
		verify(inventoryRepository).save(inventory);
	}

	@Test
	void releaseInventory_shouldAllowReleaseOfExactlyReservedQuantity() {

		Inventory exactInventory = Inventory.builder().id(1L).productId(100L).quantity(50).reservedQuantity(10).build();

		InventoryQuantityRequest exactRequest = InventoryQuantityRequest.builder().productId(100L).quantity(10).build();

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(exactInventory));

		inventoryService.releaseInventory(exactRequest);

		assertEquals(0, exactInventory.getReservedQuantity());

		verify(inventoryRepository).save(exactInventory);
	}

	@Test
	void releaseInventory_shouldThrowException_whenReleaseExceedsReservedQuantity() {

		InventoryQuantityRequest request = InventoryQuantityRequest.builder().productId(100L).quantity(11).build();

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

		InvalidInventoryOperationException exception = assertThrows(InvalidInventoryOperationException.class,
				() -> inventoryService.releaseInventory(request));

		assertEquals("Cannot release more reserved items than currently reserved.", exception.getMessage());

		assertEquals(10, inventory.getReservedQuantity());

		verify(inventoryRepository).findByProductId(100L);
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}

	@Test
	void releaseInventory_shouldThrowException_whenInventoryNotFound() {

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.empty());

		InventoryNotFoundException exception = assertThrows(InventoryNotFoundException.class,
				() -> inventoryService.releaseInventory(quantityRequest));

		assertEquals("Inventory for product id 100 not found.", exception.getMessage());

		verify(inventoryRepository).findByProductId(100L);
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}

	@Test
	void deductInventory_shouldDeductReservedStockSuccessfully() {

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

		inventoryService.deductInventory(quantityRequest);

		assertEquals(40, inventory.getQuantity());
		assertEquals(0, inventory.getReservedQuantity());

		verify(inventoryRepository).findByProductId(100L);
		verify(inventoryRepository).save(inventory);
	}

	@Test
	void deductInventory_shouldAllowDeductionOfExactlyReservedQuantity() {

		Inventory exactInventory = Inventory.builder().id(1L).productId(100L).quantity(50).reservedQuantity(10).build();

		InventoryQuantityRequest exactRequest = InventoryQuantityRequest.builder().productId(100L).quantity(10).build();

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(exactInventory));

		inventoryService.deductInventory(exactRequest);

		assertEquals(40, exactInventory.getQuantity());
		assertEquals(0, exactInventory.getReservedQuantity());

		verify(inventoryRepository).save(exactInventory);
	}

	@Test
	void deductInventory_shouldThrowException_whenDeductionExceedsReservedQuantity() {

		InventoryQuantityRequest request = InventoryQuantityRequest.builder().productId(100L).quantity(11).build();

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

		InvalidInventoryOperationException exception = assertThrows(InvalidInventoryOperationException.class,
				() -> inventoryService.deductInventory(request));

		assertEquals("Cannot deduct more reserved items than currently reserved.", exception.getMessage());

		assertEquals(50, inventory.getQuantity());
		assertEquals(10, inventory.getReservedQuantity());

		verify(inventoryRepository).findByProductId(100L);
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}

	@Test
	void deductInventory_shouldThrowException_whenInventoryNotFound() {

		when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.empty());

		InventoryNotFoundException exception = assertThrows(InventoryNotFoundException.class,
				() -> inventoryService.deductInventory(quantityRequest));

		assertEquals("Inventory for product id 100 not found.", exception.getMessage());

		verify(inventoryRepository).findByProductId(100L);
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}
}