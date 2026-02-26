package com.example.projectservice.repository;

import com.example.projectservice.entity.Invitation;
import com.example.projectservice.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    boolean existsByProjectIdAndEmailAndStatus(Long projectId, String email, InvitationStatus status);
}