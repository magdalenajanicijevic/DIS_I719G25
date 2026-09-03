package com.ecommerce.userservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.userservice.dto.LoginRequest;
import com.ecommerce.userservice.dto.LoginResponse;
import com.ecommerce.userservice.dto.RegisterRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.BadRequestException;
import com.ecommerce.userservice.exception.EmailAlreadyExistsException;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.commonsecurity.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Override
	public LoginResponse login(LoginRequest request) {

		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new BadRequestException("Invalid email or password."));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BadRequestException("Invalid email or password.");
		}

		String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());

		return LoginResponse.builder().token(token).build();

	}

	@Override
	public UserResponse register(RegisterRequest request) {

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new EmailAlreadyExistsException("Email already exists.");
		}

		User user = User.builder().firstName(request.getFirstName()).lastName(request.getLastName())
				.email(request.getEmail()).password(passwordEncoder.encode(request.getPassword())).role(Role.CUSTOMER)
				.build();

		User savedUser = userRepository.save(user);

		return UserResponse.builder().id(savedUser.getId()).firstName(savedUser.getFirstName())
				.lastName(savedUser.getLastName()).email(savedUser.getEmail()).role(savedUser.getRole())
				.createdAt(savedUser.getCreatedAt()).updatedAt(savedUser.getUpdatedAt()).build();
	}
}
