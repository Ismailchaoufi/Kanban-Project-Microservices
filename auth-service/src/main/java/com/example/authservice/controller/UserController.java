package com.example.authservice.controller;

import com.example.authservice.dto.ChangePasswordRequest;
import com.example.authservice.dto.UpdateUserRequest;
import com.example.authservice.dto.UserResponse;
import com.example.authservice.entity.Role;
import com.example.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestHeader("X-User-Role") String role,
            Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(role, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String role) {
        UserResponse user = userService.getUserById(id, requesterId, role);
        return ResponseEntity.ok(user);
    }

    /**
     * Endpoint interne pour les microservices
     * NE PAS exposer via API Gateway
     */
    @GetMapping("/internal/{userId}")
    public ResponseEntity<UserResponse> getUserByIdInternal(@PathVariable Long userId) {
        UserResponse user = userService.getUserByIdInternal(userId);
        return ResponseEntity.ok(user);
    }
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String role) {
        UserResponse user = userService.updateUser(id, request, requesterId, role);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("X-User-Id") Long requesterId) {
        userService.changePassword(id, request, requesterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String role) {
        userService.deleteUser(id, requesterId, role);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleUserStatus(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {
        userService.toggleUserStatus(id, role);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long id,
            @RequestParam Role newRole,
            @RequestHeader("X-User-Role") String role) {
        userService.updateUserRole(id, newRole, role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        UserResponse user = userService.findByEmail(email);
        return ResponseEntity.ok(user);
    }
}
