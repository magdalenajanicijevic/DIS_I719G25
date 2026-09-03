package com.ecommerce.reviewservice.integration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.reviewservice.client.OrderClient;
import com.ecommerce.reviewservice.client.ProductClient;
import com.ecommerce.reviewservice.client.UserClient;
import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.repository.ReviewRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewServiceIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReviewRepository reviewRepository;
	
	@MockitoBean
	private UserClient userClient;

	@MockitoBean
	private ProductClient productClient;

	@MockitoBean
	private OrderClient orderClient;

	@BeforeEach
	void setUp() {
		reviewRepository.deleteAll();
	}

	@Test
	void createReview_shouldCreateReviewSuccessfully() throws Exception {

		when(orderClient.hasPurchasedProduct(1L, 100L)).thenReturn(true);

		String request = """
				{
					"userId": 1,
					"productId": 100,
					"rating": 5,
					"comment": "Excellent product."
				}
				""";

		mockMvc.perform(post("/api/reviews").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.productId").value(100))
				.andExpect(jsonPath("$.rating").value(5)).andExpect(jsonPath("$.comment").value("Excellent product."));

		org.junit.jupiter.api.Assertions.assertEquals(1, reviewRepository.count());
	}

	@Test
	void createReview_shouldRejectWhenUserHasNotPurchasedProduct() throws Exception {

		when(orderClient.hasPurchasedProduct(1L, 100L)).thenReturn(false);

		String request = """
				{
					"userId": 1,
					"productId": 100,
					"rating": 5,
					"comment": "Excellent product."
				}
				""";

		mockMvc.perform(post("/api/reviews").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("User has not purchased this product."));

		org.junit.jupiter.api.Assertions.assertEquals(0, reviewRepository.count());
	}

	@Test
	void createReview_shouldRejectDuplicateReview() throws Exception {

		reviewRepository.save(Review.builder().userId(1L).productId(100L).rating(5).comment("First review.").build());

		when(orderClient.hasPurchasedProduct(1L, 100L)).thenReturn(true);

		String request = """
				{
					"userId": 1,
					"productId": 100,
					"rating": 4,
					"comment": "Second review."
				}
				""";

		mockMvc.perform(post("/api/reviews").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Review for this product already exists."));

		org.junit.jupiter.api.Assertions.assertEquals(1, reviewRepository.count());
	}

	@Test
	void createReview_shouldRejectWhenCustomerCreatesReviewForAnotherUser() throws Exception {

		String request = """
				{
					"userId": 2,
					"productId": 100,
					"rating": 5,
					"comment": "Excellent product."
				}
				""";

		mockMvc.perform(post("/api/reviews").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden());

		org.junit.jupiter.api.Assertions.assertEquals(0, reviewRepository.count());
	}

	@Test
	void createReview_shouldRejectInvalidRequest() throws Exception {

		String request = """
				{
					"userId": null,
					"productId": null,
					"rating": null,
					"comment": null
				}
				""";

		mockMvc.perform(post("/api/reviews").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());

		org.junit.jupiter.api.Assertions.assertEquals(0, reviewRepository.count());
	}

	@Test
	void updateReview_shouldUpdateOwnReview() throws Exception {

		Review review = reviewRepository
				.save(Review.builder().userId(1L).productId(100L).rating(5).comment("Excellent product.").build());

		String request = """
				{
					"rating": 4,
					"comment": "Very good product."
				}
				""";

		mockMvc.perform(put("/api/reviews/" + review.getId()).header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(review.getId())).andExpect(jsonPath("$.rating").value(4))
				.andExpect(jsonPath("$.comment").value("Very good product."));

		Review updatedReview = reviewRepository.findById(review.getId()).orElseThrow();

		org.junit.jupiter.api.Assertions.assertEquals(4, updatedReview.getRating());

		org.junit.jupiter.api.Assertions.assertEquals("Very good product.", updatedReview.getComment());
	}

	@Test
	void updateReview_shouldRejectWhenUserDoesNotOwnReview() throws Exception {

		Review review = reviewRepository
				.save(Review.builder().userId(1L).productId(100L).rating(5).comment("Excellent product.").build());

		String request = """
				{
					"rating": 2,
					"comment": "Changed by another user."
				}
				""";

		mockMvc.perform(put("/api/reviews/" + review.getId()).header("X-User-Id", "2").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden());

		Review unchangedReview = reviewRepository.findById(review.getId()).orElseThrow();

		org.junit.jupiter.api.Assertions.assertEquals(5, unchangedReview.getRating());

		org.junit.jupiter.api.Assertions.assertEquals("Excellent product.", unchangedReview.getComment());
	}

	@Test
	void updateReview_shouldReturnNotFound() throws Exception {

		String request = """
				{
					"rating": 4,
					"comment": "Updated review."
				}
				""";

		mockMvc.perform(put("/api/reviews/99999").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isNotFound());
	}

	@Test
	void deleteReview_shouldDeleteOwnReview() throws Exception {

		Review review = reviewRepository
				.save(Review.builder().userId(1L).productId(100L).rating(5).comment("Excellent product.").build());

		mockMvc.perform(
				delete("/api/reviews/" + review.getId()).header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isNoContent());

		org.junit.jupiter.api.Assertions.assertFalse(reviewRepository.existsById(review.getId()));
	}

	@Test
	void deleteReview_shouldRejectWhenCustomerDoesNotOwnReview() throws Exception {

		Review review = reviewRepository
				.save(Review.builder().userId(1L).productId(100L).rating(5).comment("Excellent product.").build());

		mockMvc.perform(
				delete("/api/reviews/" + review.getId()).header("X-User-Id", "2").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());

		org.junit.jupiter.api.Assertions.assertTrue(reviewRepository.existsById(review.getId()));
	}

	@Test
	void deleteReview_shouldAllowAdminToDeleteAnyReview() throws Exception {

		Review review = reviewRepository
				.save(Review.builder().userId(1L).productId(100L).rating(5).comment("Excellent product.").build());

		mockMvc.perform(
				delete("/api/reviews/" + review.getId()).header("X-User-Id", "2").header("X-User-Role", "ADMIN"))
				.andExpect(status().isNoContent());

		org.junit.jupiter.api.Assertions.assertFalse(reviewRepository.existsById(review.getId()));
	}

	@Test
	void deleteReview_shouldReturnNotFound() throws Exception {

		mockMvc.perform(delete("/api/reviews/99999").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getReviewById_shouldReturnReview() throws Exception {

		Review review = reviewRepository
				.save(Review.builder().userId(1L).productId(100L).rating(5).comment("Excellent product.").build());

		mockMvc.perform(get("/api/reviews/" + review.getId()).header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(review.getId()))
				.andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.productId").value(100))
				.andExpect(jsonPath("$.rating").value(5)).andExpect(jsonPath("$.comment").value("Excellent product."));
	}

	@Test
	void getReviewById_shouldReturnNotFound() throws Exception {

		mockMvc.perform(get("/api/reviews/99999").header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getReviewsByProduct_shouldReturnReviews() throws Exception {

		reviewRepository
				.save(Review.builder().userId(1L).productId(100L).rating(5).comment("Excellent product.").build());

		reviewRepository.save(Review.builder().userId(2L).productId(100L).rating(4).comment("Good product.").build());

		mockMvc.perform(get("/api/reviews/product/100").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].productId").value(100)).andExpect(jsonPath("$[1].productId").value(100));
	}

	@Test
	void getReviewsByProduct_shouldReturnEmptyList() throws Exception {

		mockMvc.perform(get("/api/reviews/product/999").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void getReviewsByUser_shouldReturnOwnReviews() throws Exception {

		reviewRepository
				.save(Review.builder().userId(1L).productId(100L).rating(5).comment("Excellent product.").build());

		reviewRepository.save(Review.builder().userId(1L).productId(200L).rating(4).comment("Good product.").build());

		mockMvc.perform(get("/api/reviews/user/1").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].userId").value(1)).andExpect(jsonPath("$[1].userId").value(1));
	}

	@Test
	void getReviewsByUser_shouldRejectCustomerAccessToAnotherUser() throws Exception {

		reviewRepository
				.save(Review.builder().userId(2L).productId(100L).rating(5).comment("Excellent product.").build());

		mockMvc.perform(get("/api/reviews/user/2").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void getReviewsByUser_shouldAllowAdminToViewAnotherUser() throws Exception {

		reviewRepository
				.save(Review.builder().userId(2L).productId(100L).rating(5).comment("Excellent product.").build());

		mockMvc.perform(get("/api/reviews/user/2").header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].userId").value(2));
	}

	@Test
	void getReviewsByUser_shouldReturnEmptyList() throws Exception {

		mockMvc.perform(get("/api/reviews/user/1").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
	}
}