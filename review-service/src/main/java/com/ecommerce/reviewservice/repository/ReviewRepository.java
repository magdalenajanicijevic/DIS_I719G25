package com.ecommerce.reviewservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.reviewservice.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	List<Review> findByProductId(Long productId);

	List<Review> findByUserId(Long userId);

	Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

	boolean existsByUserIdAndProductId(Long userId, Long productId);

}
