package com.ecommerce.reviewservice.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ReviewResponse {

	Long id;

	Long userId;

	Long productId;

	Integer rating;

	String comment;

	LocalDateTime createdAt;

	LocalDateTime updatedAt;

}
