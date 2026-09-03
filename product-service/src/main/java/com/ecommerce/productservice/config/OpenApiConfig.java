package com.ecommerce.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI productServiceOpenAPI() {

		return new OpenAPI().info(
				new Info().title("Product Service API").description("REST API for managing products.").version("1.0"));
	}

}
