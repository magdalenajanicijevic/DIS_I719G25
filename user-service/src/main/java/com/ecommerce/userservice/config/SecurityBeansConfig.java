package com.ecommerce.userservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerce.commonsecurity.config.JwtConfiguration;
import com.ecommerce.commonsecurity.config.JwtProperties;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@Import(JwtConfiguration.class)
public class SecurityBeansConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
