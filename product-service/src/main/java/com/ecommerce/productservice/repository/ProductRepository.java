package com.ecommerce.productservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.productservice.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findByName(String name);

	boolean existsByName(String name);

	List<Product> findByActiveTrue();

	Optional<Product> findByIdAndActiveTrue(Long id);

}
