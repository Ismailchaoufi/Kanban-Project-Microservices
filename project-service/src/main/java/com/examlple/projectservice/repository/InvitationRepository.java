package com.examlple.projectservice.repository;

import com.examlple.projectservice.entity.Invitation;
import com.examlple.projectservice.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    boolean existsByProjectIdAndEmailAndStatus(Long projectId, String email, InvitationStatus status);
}