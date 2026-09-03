package com.ecommerce.reviewservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecommerce.reviewservice.config.FeignConfig;

@FeignClient(name = "order-service", configuration = FeignConfig.class)
public interface OrderClient {

	@GetMapping("/api/orders/internal/purchased")
	boolean hasPurchasedProduct(@RequestParam Long userId, @RequestParam Long productId);

}
