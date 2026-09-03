package com.ecommerce.userservice.service;

import java.util.List;

import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateUserRequest;
import com.ecommerce.userservice.dto.UserResponse;

public interface UserService {

	UserResponse createUser(CreateUserRequest request, String currentUserRole);

	UserResponse getUserById(Long id, Long currentUserId, String currentUserRole);

	List<UserResponse> getAllUsers(String currentUserRole);

	UserResponse getUserByIdInternal(Long id);

	UserResponse updateUser(Long id, UpdateUserRequest request, Long currentUserId, String currentUserRole);

	void deleteUser(Long id, String currentUserRole);
}