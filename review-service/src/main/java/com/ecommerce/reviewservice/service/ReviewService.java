package com.ecommerce.reviewservice.service;

import java.util.List;

import com.ecommerce.reviewservice.dto.CreateReviewRequest;
import com.ecommerce.reviewservice.dto.ReviewResponse;
import com.ecommerce.reviewservice.dto.UpdateReviewRequest;

public interface ReviewService {

	ReviewResponse createReview(CreateReviewRequest request);

	ReviewResponse updateReview(Long id, UpdateReviewRequest request, Long currentUserId);

	void deleteReview(Long id, Long currentUserId, String role);

	ReviewResponse getReviewById(Long id);

	List<ReviewResponse> getReviewsByProduct(Long productId);

	List<ReviewResponse> getReviewsByUser(Long userId);
}