package com.ecommerce.commonsecurity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecommerce.commonsecurity.security.JwtService;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {

	@Bean JwtService jwtService(JwtProperties jwtProperties) {

		return new JwtService(jwtProperties);
	}

}
