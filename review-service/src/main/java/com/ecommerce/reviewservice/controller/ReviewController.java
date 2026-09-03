package com.ecommerce.reviewservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.reviewservice.dto.CreateReviewRequest;
import com.ecommerce.reviewservice.dto.ReviewResponse;
import com.ecommerce.reviewservice.dto.UpdateReviewRequest;
import com.ecommerce.reviewservice.exception.ForbiddenException;
import com.ecommerce.reviewservice.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Validated
@Tag(name = "Review Controller", description = "Operations related to review management")
public class ReviewController {

	private final ReviewService reviewService;

	@PostMapping
	@Operation(summary = "Create review")
	public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request,
			@RequestHeader("X-User-Id") Long currentUserId) {

		if (!request.userId().equals(currentUserId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		ReviewResponse response = reviewService.createReview(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update review")
	public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id,
			@Valid @RequestBody UpdateReviewRequest request, @RequestHeader("X-User-Id") Long currentUserId) {

		return ResponseEntity.ok(reviewService.updateReview(id, request, currentUserId));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete review")
	public ResponseEntity<Void> deleteReview(@PathVariable Long id, @RequestHeader("X-User-Id") Long currentUserId,
			@RequestHeader("X-User-Role") String role) {

		reviewService.deleteReview(id, currentUserId, role);

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get review by id")
	public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {

		return ResponseEntity.ok(reviewService.getReviewById(id));
	}

	@GetMapping("/product/{productId}")
	@Operation(summary = "Get all reviews for a product")
	public ResponseEntity<List<ReviewResponse>> getReviewsByProduct(@PathVariable Long productId) {

		return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
	}

	@GetMapping("/user/{userId}")
	@Operation(summary = "Get all reviews by user")
	public ResponseEntity<List<ReviewResponse>> getReviewsByUser(@PathVariable Long userId,
			@RequestHeader("X-User-Id") Long currentUserId, @RequestHeader("X-User-Role") String role) {

		if (!"ADMIN".equals(role) && !userId.equals(currentUserId)) {
			throw new ForbiddenException("You can only view your own reviews.");
		}

		return ResponseEntity.ok(reviewService.getReviewsByUser(userId));
	}
}