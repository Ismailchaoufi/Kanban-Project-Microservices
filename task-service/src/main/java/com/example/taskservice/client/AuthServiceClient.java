package com.example.taskservice.client;

import com.example.taskservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthServiceClient {

    @GetMapping("/api/v1/users/{id}")
    UserDTO getUserById(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String role
    );
}
