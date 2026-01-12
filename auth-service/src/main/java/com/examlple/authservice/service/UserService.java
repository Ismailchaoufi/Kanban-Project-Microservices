package com.examlple.authservice.service;

import com.examlple.authservice.dto.ChangePasswordRequest;
import com.examlple.authservice.dto.UpdateUserRequest;
import com.examlple.authservice.dto.UserResponse;
import com.examlple.authservice.entity.Role;
import com.examlple.authservice.entity.User;
import com.examlple.authservice.exception.BadRequestException;
import com.examlple.authservice.exception.ForbiddenException;
import com.examlple.authservice.exception.ResourceNotFoundException;
import com.examlple.authservice.repository.UserRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId, Long requesterId, String requesterRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check authorization: user can only see their own profile unless admin
        if (!userId.equals(requesterId) && !Role.ADMIN.name().equals(requesterRole)) {
            throw new ForbiddenException("You don't have permission to view this user");
        }

        return mapToUserResponse(user);
    }

    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return mapToUserResponse(user);
    }

    public UserResponse getUserByIdInternal(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String requesterRole, Pageable pageable) {
        // Only admins can view all users
        if (!Role.ADMIN.name().equals(requesterRole)) {
            throw new ForbiddenException("Only admins can view all users");
        }

        return userRepository.findAll(pageable).map(this::mapToUserResponse);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request, Long requesterId, String requesterRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check authorization
        if (!userId.equals(requesterId) && !Role.ADMIN.name().equals(requesterRole)) {
            throw new ForbiddenException("You don't have permission to update this user");
        }

        // Update fields if provided
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request, Long requesterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check authorization
        if (!userId.equals(requesterId)) {
            throw new ForbiddenException("You can only change your own password");
        }

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId, Long requesterId, String requesterRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check authorization
        if (!userId.equals(requesterId) && !Role.ADMIN.name().equals(requesterRole)) {
            throw new ForbiddenException("You don't have permission to delete this user");
        }

        userRepository.delete(user);
    }

    @Transactional
    public void toggleUserStatus(Long userId, String requesterRole) {
        // Only admins can toggle user status
        if (!Role.ADMIN.name().equals(requesterRole)) {
            throw new ForbiddenException("Only admins can toggle user status");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
    }

    @Transactional
    public void updateUserRole(Long userId, Role newRole, String requesterRole) {
        // Only admins can update roles
        if (!Role.ADMIN.name().equals(requesterRole)) {
            throw new ForbiddenException("Only admins can update user roles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setRole(newRole);
        userRepository.save(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }


}
