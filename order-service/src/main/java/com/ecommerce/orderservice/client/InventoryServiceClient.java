package com.ecommerce.orderservice.client;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.orderservice.dto.InventoryQuantityRequest;
import com.ecommerce.orderservice.exception.InventoryServiceUnavailableException;

import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InventoryServiceClient {

	private static final String CIRCUIT_BREAKER_NAME = "inventoryService";
	private static final String RETRY_NAME = "inventoryRead";

	private final InventoryClient inventoryClient;
	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
	private final RetryRegistry retryRegistry;

	public InventoryClientResponse getInventoryByProductId(Long productId) {

		CircuitBreaker circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);

		Retry retry = retryRegistry.retry(RETRY_NAME);

		Supplier<InventoryClientResponse> operation = Retry.decorateSupplier(retry,
				() -> inventoryClient.getInventoryByProductId(productId));

		Function<Throwable, InventoryClientResponse> fallback = throwable -> {
			throw new InventoryServiceUnavailableException("Inventory service is currently unavailable.");
		};

		return circuitBreaker.run(operation, fallback);
	}

	public void reserveInventory(InventoryQuantityRequest request) {

		runVoidOperation(() -> inventoryClient.reserveInventory(request));
	}

	public void releaseInventory(InventoryQuantityRequest request) {

		runVoidOperation(() -> inventoryClient.releaseInventory(request));
	}

	public void deductInventory(InventoryQuantityRequest request) {

		runVoidOperation(() -> inventoryClient.deductInventory(request));
	}

	private void runVoidOperation(Runnable operation) {

		CircuitBreaker circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);

		circuitBreaker.run(() -> {
			operation.run();
			return null;
		}, throwable -> {
			throw new InventoryServiceUnavailableException("Inventory service is currently unavailable.");
		});
	}
}
