package com.ecommerce.apigateway.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class RouteAuthorizationTest {

	private final RouteAuthorization routeAuthorization = new RouteAuthorization();

	@Test
	void isAuthorized_shouldAllowAdminToManageUsers() {
		assertTrue(routeAuthorization.isAuthorized("/api/users", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToReadUsers() {
		assertTrue(routeAuthorization.isAuthorized("/api/users", HttpMethod.GET, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldDenyCustomerFromCreatingUsers() {
		assertFalse(routeAuthorization.isAuthorized("/api/users", HttpMethod.POST, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToCreateUsers() {
		assertTrue(routeAuthorization.isAuthorized("/api/users", HttpMethod.POST, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToUpdateUsers() {
		assertTrue(routeAuthorization.isAuthorized("/api/users/1", HttpMethod.PUT, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToDeleteUsers() {
		assertTrue(routeAuthorization.isAuthorized("/api/users/1", HttpMethod.DELETE, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldDenyCustomerFromDeletingUsers() {
		assertFalse(routeAuthorization.isAuthorized("/api/users/1", HttpMethod.DELETE, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToReadProducts() {
		assertTrue(routeAuthorization.isAuthorized("/api/products/10", HttpMethod.GET, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToManageProducts() {
		assertTrue(routeAuthorization.isAuthorized("/api/products/1", HttpMethod.PUT, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToCreateProducts() {
		assertTrue(routeAuthorization.isAuthorized("/api/products", HttpMethod.POST, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldDenyCustomerFromCreatingProducts() {
		assertFalse(routeAuthorization.isAuthorized("/api/products", HttpMethod.POST, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldDenyCustomerFromUpdatingProducts() {
		assertFalse(routeAuthorization.isAuthorized("/api/products/1", HttpMethod.PUT, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToAccessAdminProducts() {
		assertTrue(routeAuthorization.isAuthorized("/api/products/admin", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldDenyCustomerFromAccessingAdminProducts() {
		assertFalse(routeAuthorization.isAuthorized("/api/products/admin", HttpMethod.GET, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToManageInventory() {
		assertTrue(routeAuthorization.isAuthorized("/api/inventory", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldDenyCustomerFromAccessingInventory() {
		assertFalse(routeAuthorization.isAuthorized("/api/inventory", HttpMethod.GET, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToCreateOrders() {
		assertTrue(routeAuthorization.isAuthorized("/api/orders", HttpMethod.POST, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToCreateOrders() {
		assertTrue(routeAuthorization.isAuthorized("/api/orders", HttpMethod.POST, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToAccessAllOrders() {
		assertTrue(routeAuthorization.isAuthorized("/api/orders", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldDenyCustomerFromAccessingAllOrders() {
		assertFalse(routeAuthorization.isAuthorized("/api/orders", HttpMethod.GET, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldDenyCustomerFromUpdatingOrders() {
		assertFalse(routeAuthorization.isAuthorized("/api/orders/1", HttpMethod.PUT, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToAccessPayments() {
		assertTrue(routeAuthorization.isAuthorized("/api/payments", HttpMethod.GET, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToAccessPayments() {
		assertTrue(routeAuthorization.isAuthorized("/api/payments", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToCreatePayments() {
		assertTrue(routeAuthorization.isAuthorized("/api/payments", HttpMethod.POST, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToReadProductReviews() {
		assertTrue(routeAuthorization.isAuthorized("/api/reviews/product/10", HttpMethod.GET, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToReadProductReviews() {
		assertTrue(routeAuthorization.isAuthorized("/api/reviews/product/10", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToReadOwnReviews() {
		assertTrue(routeAuthorization.isAuthorized("/api/reviews/user/10", HttpMethod.GET, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToReadUserReviews() {
		assertTrue(routeAuthorization.isAuthorized("/api/reviews/user/10", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToAccessReviewById() {
		assertTrue(routeAuthorization.isAuthorized("/api/reviews/1", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldDenyCustomerFromAccessingReviewById() {
		assertFalse(routeAuthorization.isAuthorized("/api/reviews/1", HttpMethod.GET, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToCreateReviews() {
		assertTrue(routeAuthorization.isAuthorized("/api/reviews", HttpMethod.POST, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldDenyAdminFromCreatingReviews() {
		assertFalse(routeAuthorization.isAuthorized("/api/reviews", HttpMethod.POST, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToUpdateReviews() {
		assertTrue(routeAuthorization.isAuthorized("/api/reviews/1", HttpMethod.PUT, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldDenyAdminFromUpdatingReviews() {
		assertFalse(routeAuthorization.isAuthorized("/api/reviews/1", HttpMethod.PUT, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldAllowCustomerToDeleteReviews() {
		assertTrue(routeAuthorization.isAuthorized("/api/reviews/1", HttpMethod.DELETE, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToDeleteReviews() {
		assertTrue(routeAuthorization.isAuthorized("/api/reviews/1", HttpMethod.DELETE, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldAllowAdminToAccessNotifications() {
		assertTrue(routeAuthorization.isAuthorized("/api/notifications", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldDenyCustomerFromAccessingNotifications() {
		assertFalse(routeAuthorization.isAuthorized("/api/notifications", HttpMethod.GET, "CUSTOMER"));
	}

	@Test
	void isAuthorized_shouldDenyInternalProductEndpoint() {
		assertFalse(routeAuthorization.isAuthorized("/api/products/internal/1", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldDenyInternalOrderEndpoint() {
		assertFalse(routeAuthorization.isAuthorized("/api/orders/internal/1", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldDenyInternalPurchasedEndpoint() {
		assertFalse(routeAuthorization.isAuthorized("/api/orders/internal/purchased", HttpMethod.GET, "ADMIN"));
	}

	@Test
	void isAuthorized_shouldDenyUnmappedEndpoint() {
		assertFalse(routeAuthorization.isAuthorized("/api/some-other-endpoint", HttpMethod.GET, "CUSTOMER"));
	}
}