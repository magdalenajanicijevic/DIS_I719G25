package com.ecommerce.userservice.dto;

import java.time.LocalDateTime;

import com.ecommerce.userservice.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

	private Long id;

	private String firstName;

	private String lastName;

	private String email;

	private Role role;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
