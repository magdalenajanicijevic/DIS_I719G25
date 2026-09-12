package com.ecommerce.reviewservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.reviewservice.client.OrderClient;
import com.ecommerce.reviewservice.client.ProductClient;
import com.ecommerce.reviewservice.client.UserClient;
import com.ecommerce.reviewservice.dto.CreateReviewRequest;
import com.ecommerce.reviewservice.dto.ReviewResponse;
import com.ecommerce.reviewservice.dto.UpdateReviewRequest;
import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.exception.BadRequestException;
import com.ecommerce.reviewservice.exception.ConflictException;
import com.ecommerce.reviewservice.exception.ForbiddenException;
import com.ecommerce.reviewservice.exception.ResourceNotFoundException;
import com.ecommerce.reviewservice.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

	private final ReviewRepository reviewRepository;

	private final UserClient userClient;
	private final ProductClient productClient;
	private final OrderClient orderClient;

	@Override
	public ReviewResponse createReview(CreateReviewRequest request, Long currentUserId) {

		if (!request.userId().equals(currentUserId)) {
			throw new ForbiddenException("You can only create a review for yourself.");
		}

		validateCreateReviewRequest(request);

		Review review = buildReview(request);

		Review savedReview = reviewRepository.save(review);

		return mapToResponse(savedReview);
	}

	@Override
	public ReviewResponse updateReview(Long id, UpdateReviewRequest request, Long currentUserId) {

		Review review = findReview(id);

		if (!review.getUserId().equals(currentUserId)) {
			throw new ForbiddenException("You can only modify your own review.");
		}

		review.setRating(request.rating());
		review.setComment(request.comment());

		Review updatedReview = reviewRepository.save(review);

		return mapToResponse(updatedReview);
	}

	@Override
	public void deleteReview(Long id, Long currentUserId, String role) {

		Review review = findReview(id);

		if (!"ADMIN".equals(role) && !review.getUserId().equals(currentUserId)) {

			throw new ForbiddenException("You can only delete your own review.");
		}

		reviewRepository.delete(review);
	}

	@Override
	@Transactional(readOnly = true)
	public ReviewResponse getReviewById(Long id) {

		return mapToResponse(findReview(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ReviewResponse> getReviewsByProduct(Long productId) {

		return reviewRepository.findByProductId(productId).stream().map(this::mapToResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ReviewResponse> getReviewsByUser(Long userId) {

		return reviewRepository.findByUserId(userId).stream().map(this::mapToResponse).toList();
	}

	private void validateCreateReviewRequest(CreateReviewRequest request) {

		validateUser(request.userId());

		validateProduct(request.productId());

		validatePurchase(request.userId(), request.productId());

		validateDuplicateReview(request.userId(), request.productId());
	}

	private void validateUser(Long userId) {

		userClient.getUserById(userId);
	}

	private void validateProduct(Long productId) {

		productClient.getProductById(productId);
	}

	private void validatePurchase(Long userId, Long productId) {

		if (!orderClient.hasPurchasedProduct(userId, productId)) {
			throw new BadRequestException("User has not purchased this product.");
		}
	}

	private void validateDuplicateReview(Long userId, Long productId) {

		if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
			throw new ConflictException("Review for this product already exists.");
		}
	}

	private Review buildReview(CreateReviewRequest request) {

		return Review.builder().userId(request.userId()).productId(request.productId()).rating(request.rating())
				.comment(request.comment()).build();
	}

	private Review findReview(Long id) {

		return reviewRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review not found."));
	}

	private ReviewResponse mapToResponse(Review review) {

		return ReviewResponse.builder().id(review.getId()).userId(review.getUserId()).productId(review.getProductId())
				.rating(review.getRating()).comment(review.getComment()).createdAt(review.getCreatedAt())
				.updatedAt(review.getUpdatedAt()).build();
	}
}