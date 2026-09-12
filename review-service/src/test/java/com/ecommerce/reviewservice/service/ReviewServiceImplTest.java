package com.ecommerce.reviewservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private UserClient userClient;

	@Mock
	private ProductClient productClient;

	@Mock
	private OrderClient orderClient;

	@InjectMocks
	private ReviewServiceImpl reviewService;

	private Review review;
	private CreateReviewRequest createRequest;
	private UpdateReviewRequest updateRequest;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@BeforeEach
	void setUp() {
		createdAt = LocalDateTime.of(2026, 8, 14, 10, 0);
		updatedAt = LocalDateTime.of(2026, 8, 14, 11, 0);

		createRequest = CreateReviewRequest.builder().userId(10L).productId(100L).rating(5)
				.comment("Excellent product.").build();

		updateRequest = UpdateReviewRequest.builder().rating(4).comment("Very good product.").build();

		review = Review.builder().id(1L).userId(10L).productId(100L).rating(5).comment("Excellent product.")
				.createdAt(createdAt).updatedAt(updatedAt).build();
	}

	@Test
	void createReview_shouldCreateReviewSuccessfully() {
		when(orderClient.hasPurchasedProduct(10L, 100L)).thenReturn(true);
		when(reviewRepository.existsByUserIdAndProductId(10L, 100L)).thenReturn(false);
		when(reviewRepository.save(any(Review.class))).thenReturn(review);

		ReviewResponse response = reviewService.createReview(createRequest, 10L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(10L, response.getUserId());
		assertEquals(100L, response.getProductId());
		assertEquals(5, response.getRating());
		assertEquals("Excellent product.", response.getComment());
		assertEquals(createdAt, response.getCreatedAt());
		assertEquals(updatedAt, response.getUpdatedAt());

		verify(userClient).getUserById(10L);
		verify(productClient).getProductById(100L);
		verify(orderClient).hasPurchasedProduct(10L, 100L);
		verify(reviewRepository).existsByUserIdAndProductId(10L, 100L);
		verify(reviewRepository).save(any(Review.class));
	}

	@Test
	void createReview_shouldBuildReviewWithCorrectData() {
		when(orderClient.hasPurchasedProduct(10L, 100L)).thenReturn(true);
		when(reviewRepository.existsByUserIdAndProductId(10L, 100L)).thenReturn(false);
		when(reviewRepository.save(any(Review.class))).thenReturn(review);

		reviewService.createReview(createRequest, 10L);

		ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);

		verify(reviewRepository).save(captor.capture());

		Review savedReview = captor.getValue();

		assertEquals(10L, savedReview.getUserId());
		assertEquals(100L, savedReview.getProductId());
		assertEquals(5, savedReview.getRating());
		assertEquals("Excellent product.", savedReview.getComment());
	}

	@Test
	void createReview_shouldThrowException_whenUserHasNotPurchasedProduct() {
		when(orderClient.hasPurchasedProduct(10L, 100L)).thenReturn(false);

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> reviewService.createReview(createRequest, 10L));

		assertEquals("User has not purchased this product.", exception.getMessage());

		verify(userClient).getUserById(10L);
		verify(productClient).getProductById(100L);
		verify(orderClient).hasPurchasedProduct(10L, 100L);

		verify(reviewRepository, never()).existsByUserIdAndProductId(any(), any());

		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void createReview_shouldThrowException_whenReviewAlreadyExists() {
		when(orderClient.hasPurchasedProduct(10L, 100L)).thenReturn(true);
		when(reviewRepository.existsByUserIdAndProductId(10L, 100L)).thenReturn(true);

		ConflictException exception = assertThrows(ConflictException.class,
				() -> reviewService.createReview(createRequest, 10L));

		assertEquals("Review for this product already exists.", exception.getMessage());

		verify(userClient).getUserById(10L);
		verify(productClient).getProductById(100L);
		verify(orderClient).hasPurchasedProduct(10L, 100L);
		verify(reviewRepository).existsByUserIdAndProductId(10L, 100L);

		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void createReview_shouldValidateUserBeforeProductAndPurchase() {
		when(orderClient.hasPurchasedProduct(10L, 100L)).thenReturn(true);
		when(reviewRepository.existsByUserIdAndProductId(10L, 100L)).thenReturn(false);
		when(reviewRepository.save(any(Review.class))).thenReturn(review);

		reviewService.createReview(createRequest, 10L);

		var inOrder = org.mockito.Mockito.inOrder(userClient, productClient, orderClient, reviewRepository);

		inOrder.verify(userClient).getUserById(10L);
		inOrder.verify(productClient).getProductById(100L);
		inOrder.verify(orderClient).hasPurchasedProduct(10L, 100L);
		inOrder.verify(reviewRepository).existsByUserIdAndProductId(10L, 100L);
		inOrder.verify(reviewRepository).save(any(Review.class));
	}

	@Test
	void createReview_shouldNotCheckPurchaseDuplicate_whenProductValidationFails() {
		RuntimeException exception = new RuntimeException("Product not found.");

		doThrow(exception).when(productClient).getProductById(100L);

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> reviewService.createReview(createRequest, 10L));

		assertEquals("Product not found.", thrown.getMessage());

		verify(userClient).getUserById(10L);
		verify(productClient).getProductById(100L);

		verifyNoInteractions(orderClient);
		verify(reviewRepository, never()).existsByUserIdAndProductId(any(), any());
		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void updateReview_shouldUpdateOwnReviewSuccessfully() {
		when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
		when(reviewRepository.save(review)).thenReturn(review);

		ReviewResponse response = reviewService.updateReview(1L, updateRequest, 10L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(10L, response.getUserId());
		assertEquals(100L, response.getProductId());
		assertEquals(4, response.getRating());
		assertEquals("Very good product.", response.getComment());

		assertEquals(4, review.getRating());
		assertEquals("Very good product.", review.getComment());

		verify(reviewRepository).findById(1L);
		verify(reviewRepository).save(review);
	}

	@Test
	void updateReview_shouldRejectWhenUserDoesNotOwnReview() {
		when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> reviewService.updateReview(1L, updateRequest, 20L));

		assertEquals("You can only modify your own review.", exception.getMessage());

		verify(reviewRepository).findById(1L);
		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void updateReview_shouldThrowException_whenReviewNotFound() {
		when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> reviewService.updateReview(1L, updateRequest, 10L));

		assertEquals("Review not found.", exception.getMessage());

		verify(reviewRepository).findById(1L);
		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void deleteReview_shouldDeleteOwnReviewSuccessfully() {
		when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

		reviewService.deleteReview(1L, 10L, "CUSTOMER");

		verify(reviewRepository).findById(1L);
		verify(reviewRepository).delete(review);
	}

	@Test
	void deleteReview_shouldAllowAdminToDeleteAnyReview() {
		when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

		reviewService.deleteReview(1L, 20L, "ADMIN");

		verify(reviewRepository).findById(1L);
		verify(reviewRepository).delete(review);
	}

	@Test
	void deleteReview_shouldRejectCustomerDeletingAnotherUsersReview() {
		when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> reviewService.deleteReview(1L, 20L, "CUSTOMER"));

		assertEquals("You can only delete your own review.", exception.getMessage());

		verify(reviewRepository).findById(1L);
		verify(reviewRepository, never()).delete(any(Review.class));
	}

	@Test
	void deleteReview_shouldThrowException_whenReviewNotFound() {
		when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> reviewService.deleteReview(1L, 10L, "CUSTOMER"));

		assertEquals("Review not found.", exception.getMessage());

		verify(reviewRepository).findById(1L);
		verify(reviewRepository, never()).delete(any(Review.class));
	}

	@Test
	void getReviewById_shouldReturnReviewSuccessfully() {
		when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

		ReviewResponse response = reviewService.getReviewById(1L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(10L, response.getUserId());
		assertEquals(100L, response.getProductId());
		assertEquals(5, response.getRating());
		assertEquals("Excellent product.", response.getComment());
		assertEquals(createdAt, response.getCreatedAt());
		assertEquals(updatedAt, response.getUpdatedAt());

		verify(reviewRepository).findById(1L);
	}

	@Test
	void getReviewById_shouldThrowException_whenReviewNotFound() {
		when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> reviewService.getReviewById(1L));

		assertEquals("Review not found.", exception.getMessage());

		verify(reviewRepository).findById(1L);
	}

	@Test
	void getReviewsByProduct_shouldReturnReviews() {
		Review secondReview = Review.builder().id(2L).userId(20L).productId(100L).rating(4).comment("Good product.")
				.createdAt(createdAt).updatedAt(updatedAt).build();

		when(reviewRepository.findByProductId(100L)).thenReturn(List.of(review, secondReview));

		List<ReviewResponse> response = reviewService.getReviewsByProduct(100L);

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals(1L, response.get(0).getId());
		assertEquals(2L, response.get(1).getId());

		assertEquals(100L, response.get(0).getProductId());
		assertEquals(100L, response.get(1).getProductId());

		verify(reviewRepository).findByProductId(100L);
	}

	@Test
	void getReviewsByProduct_shouldReturnEmptyList_whenNoReviewsExist() {
		when(reviewRepository.findByProductId(100L)).thenReturn(List.of());

		List<ReviewResponse> response = reviewService.getReviewsByProduct(100L);

		assertNotNull(response);
		assertEquals(0, response.size());

		verify(reviewRepository).findByProductId(100L);
	}

	@Test
	void getReviewsByUser_shouldReturnReviews() {
		Review secondReview = Review.builder().id(2L).userId(10L).productId(200L).rating(4).comment("Good product.")
				.createdAt(createdAt).updatedAt(updatedAt).build();

		when(reviewRepository.findByUserId(10L)).thenReturn(List.of(review, secondReview));

		List<ReviewResponse> response = reviewService.getReviewsByUser(10L);

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals(1L, response.get(0).getId());
		assertEquals(2L, response.get(1).getId());

		assertEquals(10L, response.get(0).getUserId());
		assertEquals(10L, response.get(1).getUserId());

		verify(reviewRepository).findByUserId(10L);
	}

	@Test
	void getReviewsByUser_shouldReturnEmptyList_whenNoReviewsExist() {
		when(reviewRepository.findByUserId(10L)).thenReturn(List.of());

		List<ReviewResponse> response = reviewService.getReviewsByUser(10L);

		assertNotNull(response);
		assertEquals(0, response.size());

		verify(reviewRepository).findByUserId(10L);
	}
}