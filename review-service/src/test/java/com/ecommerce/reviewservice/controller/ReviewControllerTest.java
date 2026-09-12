package com.ecommerce.reviewservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ecommerce.reviewservice.dto.CreateReviewRequest;
import com.ecommerce.reviewservice.dto.ReviewResponse;
import com.ecommerce.reviewservice.dto.UpdateReviewRequest;
import com.ecommerce.reviewservice.exception.ForbiddenException;
import com.ecommerce.reviewservice.exception.GlobalExceptionHandler;
import com.ecommerce.reviewservice.exception.ResourceNotFoundException;
import com.ecommerce.reviewservice.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

	@Mock
	private ReviewService reviewService;

	@InjectMocks
	private ReviewController reviewController;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	private ReviewResponse reviewResponse;
	private CreateReviewRequest createRequest;
	private UpdateReviewRequest updateRequest;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(reviewController).setControllerAdvice(new GlobalExceptionHandler())
				.build();

		objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();

		createRequest = CreateReviewRequest.builder().userId(1L).productId(100L).rating(5).comment("Excellent product.")
				.build();

		updateRequest = UpdateReviewRequest.builder().rating(4).comment("Very good product.").build();

		reviewResponse = ReviewResponse.builder().id(1L).userId(1L).productId(100L).rating(5)
				.comment("Excellent product.").createdAt(LocalDateTime.of(2026, 8, 15, 10, 0))
				.updatedAt(LocalDateTime.of(2026, 8, 15, 10, 0)).build();
	}

	@Test
	void createReview_shouldReturnCreated() throws Exception {
		when(reviewService.createReview(any(CreateReviewRequest.class), eq(1L))).thenReturn(reviewResponse);

		mockMvc.perform(post("/api/reviews").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(createRequest)))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.userId").value(1))
				.andExpect(jsonPath("$.productId").value(100)).andExpect(jsonPath("$.rating").value(5))
				.andExpect(jsonPath("$.comment").value("Excellent product."));

		verify(reviewService).createReview(any(CreateReviewRequest.class), eq(1L));
	}

	@Test
	void createReview_shouldReturnForbidden_whenUserCreatesReviewForAnotherUser() throws Exception {
		CreateReviewRequest request = CreateReviewRequest.builder().userId(2L).productId(100L).rating(5)
				.comment("Excellent product.").build();

		when(reviewService.createReview(any(CreateReviewRequest.class), eq(1L)))
				.thenThrow(new ForbiddenException("You can only create a review for yourself."));

		mockMvc.perform(post("/api/reviews").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("You can only create a review for yourself."));

		verify(reviewService).createReview(any(CreateReviewRequest.class), eq(1L));
	}

	@Test
	void createReview_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
		CreateReviewRequest request = CreateReviewRequest.builder().userId(null).productId(null).rating(null)
				.comment(null).build();

		mockMvc.perform(post("/api/reviews").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		verify(reviewService, org.mockito.Mockito.never()).createReview(any(CreateReviewRequest.class),
				any(Long.class));
	}

	@Test
	void updateReview_shouldReturnOk_whenUserOwnsReview() throws Exception {
		ReviewResponse updatedResponse = ReviewResponse.builder().id(1L).userId(1L).productId(100L).rating(4)
				.comment("Very good product.").createdAt(reviewResponse.getCreatedAt())
				.updatedAt(reviewResponse.getUpdatedAt()).build();

		when(reviewService.updateReview(1L, updateRequest, 1L)).thenReturn(updatedResponse);

		mockMvc.perform(put("/api/reviews/1").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.rating").value(4))
				.andExpect(jsonPath("$.comment").value("Very good product."));

		verify(reviewService).updateReview(1L, updateRequest, 1L);
	}

	@Test
	void updateReview_shouldReturnForbidden_whenUserDoesNotOwnReview() throws Exception {
		when(reviewService.updateReview(1L, updateRequest, 2L))
				.thenThrow(new ForbiddenException("You can only modify your own review."));

		mockMvc.perform(put("/api/reviews/1").header("X-User-Id", "2").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest)))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("You can only modify your own review."));

		verify(reviewService).updateReview(1L, updateRequest, 2L);
	}

	@Test
	void updateReview_shouldReturnNotFound_whenReviewDoesNotExist() throws Exception {
		when(reviewService.updateReview(1L, updateRequest, 1L))
				.thenThrow(new ResourceNotFoundException("Review not found."));

		mockMvc.perform(put("/api/reviews/1").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));

		verify(reviewService).updateReview(1L, updateRequest, 1L);
	}

	@Test
	void updateReview_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
		UpdateReviewRequest request = UpdateReviewRequest.builder().rating(null).comment(null).build();

		mockMvc.perform(put("/api/reviews/1").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		verify(reviewService, org.mockito.Mockito.never()).updateReview(any(Long.class), any(UpdateReviewRequest.class),
				any(Long.class));
	}

	@Test
	void deleteReview_shouldReturnNoContent_whenCustomerOwnsReview() throws Exception {
		doNothing().when(reviewService).deleteReview(1L, 1L, "CUSTOMER");

		mockMvc.perform(delete("/api/reviews/1").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isNoContent());

		verify(reviewService).deleteReview(1L, 1L, "CUSTOMER");
	}

	@Test
	void deleteReview_shouldReturnNoContent_whenAdminDeletesReview() throws Exception {
		doNothing().when(reviewService).deleteReview(1L, 2L, "ADMIN");

		mockMvc.perform(delete("/api/reviews/1").header("X-User-Id", "2").header("X-User-Role", "ADMIN"))
				.andExpect(status().isNoContent());

		verify(reviewService).deleteReview(1L, 2L, "ADMIN");
	}

	@Test
	void deleteReview_shouldReturnForbidden_whenCustomerDoesNotOwnReview() throws Exception {
		doThrow(new ForbiddenException("You can only delete your own review.")).when(reviewService).deleteReview(1L, 2L,
				"CUSTOMER");

		mockMvc.perform(delete("/api/reviews/1").header("X-User-Id", "2").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("You can only delete your own review."));

		verify(reviewService).deleteReview(1L, 2L, "CUSTOMER");
	}

	@Test
	void deleteReview_shouldReturnNotFound_whenReviewDoesNotExist() throws Exception {
		doThrow(new ResourceNotFoundException("Review not found.")).when(reviewService).deleteReview(999L, 1L,
				"CUSTOMER");

		mockMvc.perform(delete("/api/reviews/999").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));

		verify(reviewService).deleteReview(999L, 1L, "CUSTOMER");
	}

	@Test
	void getReviewById_shouldReturnOk() throws Exception {
		when(reviewService.getReviewById(1L)).thenReturn(reviewResponse);

		mockMvc.perform(get("/api/reviews/1").header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.productId").value(100))
				.andExpect(jsonPath("$.rating").value(5));

		verify(reviewService).getReviewById(1L);
	}

	@Test
	void getReviewById_shouldReturnNotFound_whenReviewDoesNotExist() throws Exception {
		when(reviewService.getReviewById(999L)).thenThrow(new ResourceNotFoundException("Review not found."));

		mockMvc.perform(get("/api/reviews/999").header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));

		verify(reviewService).getReviewById(999L);
	}

	@Test
	void getReviewsByProduct_shouldReturnReviews() throws Exception {
		ReviewResponse secondResponse = ReviewResponse.builder().id(2L).userId(2L).productId(100L).rating(4)
				.comment("Good product.").createdAt(reviewResponse.getCreatedAt())
				.updatedAt(reviewResponse.getUpdatedAt()).build();

		when(reviewService.getReviewsByProduct(100L)).thenReturn(List.of(reviewResponse, secondResponse));

		mockMvc.perform(get("/api/reviews/product/100").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(1)).andExpect(jsonPath("$[1].id").value(2))
				.andExpect(jsonPath("$[0].productId").value(100)).andExpect(jsonPath("$[1].productId").value(100));

		verify(reviewService).getReviewsByProduct(100L);
	}

	@Test
	void getReviewsByProduct_shouldReturnEmptyList_whenNoReviewsExist() throws Exception {
		when(reviewService.getReviewsByProduct(100L)).thenReturn(List.of());

		mockMvc.perform(get("/api/reviews/product/100").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

		verify(reviewService).getReviewsByProduct(100L);
	}

	@Test
	void getReviewsByUser_shouldReturnOwnReviews() throws Exception {
		when(reviewService.getReviewsByUser(1L)).thenReturn(List.of(reviewResponse));

		mockMvc.perform(get("/api/reviews/user/1").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(1)).andExpect(jsonPath("$[0].userId").value(1));

		verify(reviewService).getReviewsByUser(1L);
	}

	@Test
	void getReviewsByUser_shouldReturnForbidden_whenCustomerRequestsAnotherUsersReviews() throws Exception {
		mockMvc.perform(get("/api/reviews/user/2").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());

		verify(reviewService, org.mockito.Mockito.never()).getReviewsByUser(any(Long.class));
	}

	@Test
	void getReviewsByUser_shouldAllowAdminToViewAnotherUsersReviews() throws Exception {
		when(reviewService.getReviewsByUser(2L)).thenReturn(List.of());

		mockMvc.perform(get("/api/reviews/user/2").header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

		verify(reviewService).getReviewsByUser(2L);
	}

	@Test
	void getReviewsByUser_shouldReturnEmptyList_whenCustomerHasNoReviews() throws Exception {
		when(reviewService.getReviewsByUser(1L)).thenReturn(List.of());

		mockMvc.perform(get("/api/reviews/user/1").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

		verify(reviewService).getReviewsByUser(1L);
	}
}