package com.ecommerce.reviewservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;


@Builder
public record CreateReviewRequest(

		@NotNull Long userId,

		@NotNull Long productId,

		@NotNull @Min(1) @Max(5) Integer rating,

		@NotBlank String comment

) {
}
