package com.ecommerce.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.ecommerce.apigateway.exception.GatewayExceptionHandler;
import com.ecommerce.apigateway.security.RouteAuthorization;
import com.ecommerce.apigateway.security.RouteValidator;
import com.ecommerce.commonsecurity.security.JwtService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter {

	private final RouteValidator routeValidator;
	private final JwtService jwtService;
	private final GatewayExceptionHandler exceptionHandler;
	private final RouteAuthorization routeAuthorization;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		if (!routeValidator.isSecured(exchange)) {
			return chain.filter(exchange);
		}

		String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return exceptionHandler.writeError(exchange, HttpStatus.UNAUTHORIZED,
					"Missing or invalid Authorization header.");
		}

		String token = authHeader.substring(7);

		try {

			String username = jwtService.extractUsername(token);

			if (!jwtService.isTokenValid(token, username)) {
				return exceptionHandler.writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid JWT token.");
			}

			String role = jwtService.extractRole(token);

			String requestPath = exchange.getRequest().getURI().getPath();

			if (!routeAuthorization.isAuthorized(requestPath, exchange.getRequest().getMethod(), role)) {
				return exceptionHandler.writeError(exchange, HttpStatus.FORBIDDEN, "Access denied.");
			}

			String userId = String.valueOf(jwtService.extractUserId(token));

			ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
				headers.remove("X-User-Id");
				headers.remove("X-User-Role");
				headers.remove("X-User-Email");
			}).header("X-User-Id", userId).header("X-User-Role", role).header("X-User-Email", username).build();

			return chain.filter(exchange.mutate().request(request).build());

		} catch (Exception ex) {

			return exceptionHandler.writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token.");
		}
	}
}