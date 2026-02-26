package com.example.projectservice.controller;

import com.example.projectservice.dto.InvitationInfoResponse;
import com.example.projectservice.service.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;

    /**
     * PUBLIC - Vérifier invitation
     */
    @GetMapping("/verify")
    public ResponseEntity<InvitationInfoResponse> verify(@RequestParam String token) {
        log.info("Verifying invitation token: {}", token);
        InvitationInfoResponse info = invitationService.getInvitationInfo(token);
        return ResponseEntity.ok(info);
    }

    /**
     * PROTÉGÉ - Accepter invitation
     */
    @PostMapping("/accept")
    public ResponseEntity<String> accept(
            @RequestParam String token,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("Accepting invitation - Token: {}, UserId: {}", token, userId);
        invitationService.acceptInvitation(token, userId);
        return ResponseEntity.ok("Invitation accepted successfully");
    }
}