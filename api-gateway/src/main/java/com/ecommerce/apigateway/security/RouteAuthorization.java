package com.ecommerce.apigateway.security;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class RouteAuthorization {

	private static final Map<String, List<String>> PERMISSIONS = Map.ofEntries(

			Map.entry("GET:/api/users", List.of("ADMIN", "CUSTOMER")), Map.entry("POST:/api/users", List.of("ADMIN")),
			Map.entry("PUT:/api/users", List.of("ADMIN", "CUSTOMER")), Map.entry("DELETE:/api/users", List.of("ADMIN")),

			Map.entry("GET:/api/products", List.of("ADMIN", "CUSTOMER")),
			Map.entry("GET:/api/products/admin", List.of("ADMIN")), Map.entry("POST:/api/products", List.of("ADMIN")),
			Map.entry("PUT:/api/products", List.of("ADMIN")), Map.entry("DELETE:/api/products", List.of("ADMIN")),

			Map.entry("GET:/api/inventory", List.of("ADMIN")), Map.entry("POST:/api/inventory", List.of("ADMIN")),
			Map.entry("PUT:/api/inventory", List.of("ADMIN")), Map.entry("DELETE:/api/inventory", List.of("ADMIN")),

			Map.entry("GET:/api/orders", List.of("ADMIN", "CUSTOMER")), Map.entry("POST:/api/orders", List.of("ADMIN", "CUSTOMER")),
			Map.entry("PUT:/api/orders", List.of("ADMIN")), Map.entry("DELETE:/api/orders", List.of("ADMIN")),

			Map.entry("GET:/api/payments", List.of("ADMIN", "CUSTOMER")),
			Map.entry("POST:/api/payments", List.of("ADMIN", "CUSTOMER")),

			Map.entry("GET:/api/reviews/product", List.of("ADMIN", "CUSTOMER")),
			Map.entry("GET:/api/reviews/user", List.of("ADMIN", "CUSTOMER")),
			Map.entry("GET:/api/reviews", List.of("ADMIN", "CUSTOMER")), Map.entry("POST:/api/reviews", List.of("CUSTOMER")),
			Map.entry("PUT:/api/reviews", List.of("CUSTOMER")),
			Map.entry("DELETE:/api/reviews", List.of("ADMIN", "CUSTOMER")),

			Map.entry("GET:/api/notifications", List.of("ADMIN")));

	public boolean isAuthorized(String path, HttpMethod method, String role) {

		if (isInternalEndpoint(path)) {
			return false;
		}

		String basePath = findBasePath(path);

		String key = method.name() + ":" + basePath;

		List<String> allowedRoles = PERMISSIONS.get(key);

		if (allowedRoles == null) {
			return false;
		}

		return allowedRoles.contains(role);
	}

	private boolean isInternalEndpoint(String path) {

		return path.matches(".*/internal(?:/.*)?");
	}

	private String findBasePath(String path) {

		return PERMISSIONS.keySet().stream().map(key -> key.substring(key.indexOf(":") + 1)).filter(path::startsWith)
				.max(Comparator.comparingInt(String::length)).orElse(path);
	}
}