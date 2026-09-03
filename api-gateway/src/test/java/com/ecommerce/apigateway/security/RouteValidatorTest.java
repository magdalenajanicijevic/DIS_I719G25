package com.ecommerce.apigateway.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class RouteValidatorTest {

	private final RouteValidator routeValidator = new RouteValidator();

	@Test
	void isSecured_shouldReturnFalse_forAuthEndpoint() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login").build());

		assertFalse(routeValidator.isSecured(exchange));
	}

	@Test
	void isSecured_shouldReturnFalse_forSwaggerEndpoint() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/swagger-ui/index.html").build());

		assertFalse(routeValidator.isSecured(exchange));
	}

	@Test
	void isSecured_shouldReturnFalse_forApiDocsEndpoint() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v3/api-docs").build());

		assertFalse(routeValidator.isSecured(exchange));
	}

	@Test
	void isSecured_shouldReturnFalse_forActuatorEndpoint() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());

		assertFalse(routeValidator.isSecured(exchange));
	}

	@Test
	void isSecured_shouldReturnTrue_forProtectedEndpoint() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products/1").build());

		assertTrue(routeValidator.isSecured(exchange));
	}

	@Test
	void isSecured_shouldReturnTrue_forReviewEndpoint() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/reviews/product/1").build());

		assertTrue(routeValidator.isSecured(exchange));
	}
}