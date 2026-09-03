package com.ecommerce.orderservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.orderservice.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
	
	Optional<Order> findByIdAndDeletedAtIsNull(Long id);

	List<Order> findByDeletedAtIsNull();

	List<Order> findByUserIdAndDeletedAtIsNull(Long userId);



}
