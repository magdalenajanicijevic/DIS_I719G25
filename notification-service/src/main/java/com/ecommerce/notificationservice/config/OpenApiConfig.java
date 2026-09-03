package com.ecommerce.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI notificationServiceOpenAPI() {

		return new OpenAPI().info(new Info().title("Notification Service API")
				.description("REST API for managing notifications.").version("1.0"));
	}

}
