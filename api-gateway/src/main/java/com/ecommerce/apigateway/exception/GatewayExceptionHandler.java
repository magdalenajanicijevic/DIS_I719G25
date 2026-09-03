package com.ecommerce.apigateway.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class GatewayExceptionHandler {

	private final ObjectMapper objectMapper;

	public Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {

		try {

			ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).status(status.value())
					.error(status.getReasonPhrase()).message(message).build();

			byte[] body = objectMapper.writeValueAsBytes(error);

			exchange.getResponse().setStatusCode(status);
			exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

			return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));

		} catch (Exception ex) {

			ex.printStackTrace();

			exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return exchange.getResponse().setComplete();
		}
	}
}