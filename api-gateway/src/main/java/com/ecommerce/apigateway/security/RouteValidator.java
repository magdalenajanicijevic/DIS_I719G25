package com.ecommerce.apigateway.security;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class RouteValidator {

	private static final List<String> openEndpoints = List.of(

			"/api/auth",

			"/swagger-ui",

			"/v3/api-docs",

			"/actuator"

	);

	public boolean isSecured(ServerWebExchange exchange) {

		String path = exchange.getRequest().getURI().getPath();

		return openEndpoints.stream()

				.noneMatch(path::startsWith);

	}

}
