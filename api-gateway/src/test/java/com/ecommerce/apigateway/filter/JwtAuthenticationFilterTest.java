package com.ecommerce.apigateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.ecommerce.apigateway.exception.GatewayExceptionHandler;
import com.ecommerce.apigateway.security.RouteAuthorization;
import com.ecommerce.apigateway.security.RouteValidator;
import com.ecommerce.commonsecurity.security.JwtService;

import reactor.core.publisher.Mono;

class JwtAuthenticationFilterTest {

	@Mock
	private RouteValidator routeValidator;

	@Mock
	private JwtService jwtService;

	@Mock
	private GatewayExceptionHandler exceptionHandler;

	@Mock
	private RouteAuthorization routeAuthorization;

	@Mock
	private GatewayFilterChain chain;

	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		filter = new JwtAuthenticationFilter(routeValidator, jwtService, exceptionHandler, routeAuthorization);
	}

	@Test
	void filter_shouldContinueChain_forPublicEndpoint() {

		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login").build());

		when(routeValidator.isSecured(exchange)).thenReturn(false);
		when(chain.filter(exchange)).thenReturn(Mono.empty());

		filter.filter(exchange, chain).block();

		verify(chain).filter(exchange);
		verifyNoJwtValidation();
	}

	@Test
	void filter_shouldReturnUnauthorized_whenAuthorizationHeaderIsMissing() {

		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products").build());

		when(routeValidator.isSecured(exchange)).thenReturn(true);

		when(exceptionHandler.writeError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header."))
				.thenReturn(Mono.empty());

		filter.filter(exchange, chain).block();

		verify(exceptionHandler).writeError(exchange, HttpStatus.UNAUTHORIZED,
				"Missing or invalid Authorization header.");

		verify(chain, never()).filter(any());
	}

	@Test
	void filter_shouldReturnUnauthorized_whenAuthorizationHeaderIsInvalid() {

		var exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/products").header(HttpHeaders.AUTHORIZATION, "Basic abc").build());

		when(routeValidator.isSecured(exchange)).thenReturn(true);

		when(exceptionHandler.writeError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header."))
				.thenReturn(Mono.empty());

		filter.filter(exchange, chain).block();

		verify(exceptionHandler).writeError(exchange, HttpStatus.UNAUTHORIZED,
				"Missing or invalid Authorization header.");

		verify(chain, never()).filter(any());
	}

	@Test
	void filter_shouldReturnUnauthorized_whenTokenIsInvalid() {

		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products")
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token").build());

		when(routeValidator.isSecured(exchange)).thenReturn(true);
		when(jwtService.extractUsername("invalid-token")).thenReturn("user@example.com");
		when(jwtService.isTokenValid("invalid-token", "user@example.com")).thenReturn(false);

		when(exceptionHandler.writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid JWT token."))
				.thenReturn(Mono.empty());

		filter.filter(exchange, chain).block();

		verify(exceptionHandler).writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid JWT token.");

		verify(chain, never()).filter(any());
	}

	@Test
	void filter_shouldReturnForbidden_whenUserIsNotAuthorized() {

		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token").build());

		when(routeValidator.isSecured(exchange)).thenReturn(true);
		when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
		when(jwtService.isTokenValid("valid-token", "user@example.com")).thenReturn(true);
		when(jwtService.extractRole("valid-token")).thenReturn("CUSTOMER");
		when(routeAuthorization.isAuthorized("/api/products", HttpMethod.GET, "CUSTOMER")).thenReturn(false);

		when(exceptionHandler.writeError(exchange, HttpStatus.FORBIDDEN, "Access denied.")).thenReturn(Mono.empty());

		filter.filter(exchange, chain).block();

		verify(exceptionHandler).writeError(exchange, HttpStatus.FORBIDDEN, "Access denied.");

		verify(chain, never()).filter(any());
	}

	@Test
	void filter_shouldContinueChain_whenUserIsAuthorized() {

		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token").build());

		when(routeValidator.isSecured(exchange)).thenReturn(true);
		when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
		when(jwtService.isTokenValid("valid-token", "user@example.com")).thenReturn(true);
		when(jwtService.extractRole("valid-token")).thenReturn("CUSTOMER");
		when(routeAuthorization.isAuthorized("/api/products", HttpMethod.GET, "CUSTOMER")).thenReturn(true);
		when(jwtService.extractUserId("valid-token")).thenReturn(10L);
		when(chain.filter(any())).thenReturn(Mono.empty());

		filter.filter(exchange, chain).block();

		verify(chain).filter(any());
	}

	@Test
	void filter_shouldAddUserHeaders_whenUserIsAuthorized() {

		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token").build());

		when(routeValidator.isSecured(exchange)).thenReturn(true);
		when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
		when(jwtService.isTokenValid("valid-token", "user@example.com")).thenReturn(true);
		when(jwtService.extractRole("valid-token")).thenReturn("CUSTOMER");
		when(jwtService.extractUserId("valid-token")).thenReturn(10L);
		when(routeAuthorization.isAuthorized("/api/products", HttpMethod.GET, "CUSTOMER")).thenReturn(true);
		when(chain.filter(any())).thenReturn(Mono.empty());

		filter.filter(exchange, chain).block();

		ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);

		verify(chain).filter(captor.capture());

		ServerHttpRequest request = captor.getValue().getRequest();

		assertEquals("10", request.getHeaders().getFirst("X-User-Id"));

		assertEquals("CUSTOMER", request.getHeaders().getFirst("X-User-Role"));

		assertEquals("user@example.com", request.getHeaders().getFirst("X-User-Email"));
	}

	@Test
	void filter_shouldRemoveExistingUserHeaders_whenUserIsAuthorized() {

		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token").header("X-User-Id", "999")
				.header("X-User-Role", "ADMIN").header("X-User-Email", "fake@example.com").build());

		when(routeValidator.isSecured(exchange)).thenReturn(true);
		when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
		when(jwtService.isTokenValid("valid-token", "user@example.com")).thenReturn(true);
		when(jwtService.extractRole("valid-token")).thenReturn("CUSTOMER");
		when(jwtService.extractUserId("valid-token")).thenReturn(10L);
		when(routeAuthorization.isAuthorized("/api/products", HttpMethod.GET, "CUSTOMER")).thenReturn(true);
		when(chain.filter(any())).thenReturn(Mono.empty());

		filter.filter(exchange, chain).block();

		ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);

		verify(chain).filter(captor.capture());

		ServerHttpRequest request = captor.getValue().getRequest();

		assertEquals("10", request.getHeaders().getFirst("X-User-Id"));

		assertEquals("CUSTOMER", request.getHeaders().getFirst("X-User-Role"));

		assertEquals("user@example.com", request.getHeaders().getFirst("X-User-Email"));
	}

	@Test
	void filter_shouldReturnUnauthorized_whenJwtProcessingThrowsException() {

		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products")
				.header(HttpHeaders.AUTHORIZATION, "Bearer broken-token").build());

		when(routeValidator.isSecured(exchange)).thenReturn(true);
		when(jwtService.extractUsername("broken-token")).thenThrow(new RuntimeException("JWT error"));

		when(exceptionHandler.writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token."))
				.thenReturn(Mono.empty());

		filter.filter(exchange, chain).block();

		verify(exceptionHandler).writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token.");

		verify(chain, never()).filter(any());
	}

	private void verifyNoJwtValidation() {

		verify(jwtService, never()).extractUsername(any());
		verify(jwtService, never()).isTokenValid(any(), any());
		verify(jwtService, never()).extractRole(any());
	}
}