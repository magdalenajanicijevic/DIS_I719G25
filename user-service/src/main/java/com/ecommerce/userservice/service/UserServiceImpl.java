package com.ecommerce.userservice.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateUserRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.EmailAlreadyExistsException;
import com.ecommerce.userservice.exception.ForbiddenException;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public UserResponse createUser(CreateUserRequest request, String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can create users.");
		}

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new EmailAlreadyExistsException("Email already exists.");
		}

		User user = User.builder().firstName(request.getFirstName()).lastName(request.getLastName())
				.email(request.getEmail()).password(passwordEncoder.encode(request.getPassword()))
				.role(request.getRole()).build();

		User savedUser = userRepository.save(user);

		return mapToResponse(savedUser);
	}

	@Override
	public UserResponse getUserById(Long id, Long currentUserId, String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole) && !id.equals(currentUserId)) {
			throw new ForbiddenException("You can only access your own user data.");
		}

		User user = findUserById(id);

		return mapToResponse(user);
	}

	@Override
	public List<UserResponse> getAllUsers(String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can access all users.");
		}

		return userRepository.findAll().stream().map(this::mapToResponse).toList();
	}

	@Override
	public UserResponse getUserByIdInternal(Long id) {

		return mapToResponse(findUserById(id));
	}

	@Override
	public UserResponse updateUser(Long id, UpdateUserRequest request, Long currentUserId, String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole) && !id.equals(currentUserId)) {
			throw new ForbiddenException("You can only update your own user data.");
		}

		User user = findUserById(id);

		if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {

			throw new EmailAlreadyExistsException("Email already exists.");
		}

		user.setFirstName(request.getFirstName());
		user.setLastName(request.getLastName());
		user.setEmail(request.getEmail());

		if ("ADMIN".equals(currentUserRole)) {
			user.setRole(request.getRole());
		}

		User updatedUser = userRepository.save(user);

		return mapToResponse(updatedUser);
	}

	@Override
	public void deleteUser(Long id, String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can delete users.");
		}

		User user = findUserById(id);

		userRepository.delete(user);
	}

	private User findUserById(Long id) {

		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found."));
	}

	private UserResponse mapToResponse(User user) {

		return UserResponse.builder().id(user.getId()).firstName(user.getFirstName()).lastName(user.getLastName())
				.email(user.getEmail()).role(user.getRole()).createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt()).build();
	}
}