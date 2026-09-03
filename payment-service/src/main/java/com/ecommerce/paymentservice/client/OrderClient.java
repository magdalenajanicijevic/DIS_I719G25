package com.ecommerce.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ecommerce.paymentservice.config.FeignConfig;

@FeignClient(name = "order-service", configuration = FeignConfig.class)
public interface OrderClient {

    @GetMapping("/api/orders/internal/{id}")
    OrderClientResponse getOrderById(@PathVariable Long id);

}