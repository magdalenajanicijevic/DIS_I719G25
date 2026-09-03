package com.ecommerce.userservice.dto;

import lombok.Builder;

@Builder
public record LoginResponse(

		String token

) {
}
