package com.example.projectservice.service;

import com.example.projectservice.entity.Invitation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendInvitationEmail(Invitation invitation, String projectTitle) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(invitation.getEmail());
            message.setSubject("Invitation to join project: " + projectTitle);
            message.setText(buildInvitationEmailBody(invitation, projectTitle));

            mailSender.send(message);
            log.info("Invitation email sent to {}", invitation.getEmail());

        } catch (Exception e) {
            log.error("Failed to send invitation email to {}", invitation.getEmail(), e);
        }
    }

    @Async
    public void sendDirectAddEmail(String email, Long projectId, String projectTitle) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("You've been added to project: " + projectTitle);
            message.setText(buildDirectAddEmailBody(projectId, projectTitle));

            mailSender.send(message);
            log.info("Direct add notification sent to {}", email);

        } catch (Exception e) {
            log.error("Failed to send direct add email to {}", email, e);
        }
    }

    private String buildInvitationEmailBody(Invitation invitation, String projectTitle) {
        return String.format("""
        Hello,
        
        You have been invited to join the project "%s".
        
        Click the link below to accept this invitation:
        %s/invitations/accept?token=%s
        
        This invitation will expire in 7 days.
        
        Best regards,
        The Team
        """,
                projectTitle,
                frontendUrl,
                invitation.getToken()
        );
    }

    private String buildDirectAddEmailBody(Long projectId, String projectTitle) {
        return String.format("""
        Hello,
        
        You have been added to the project "%s".
        
        View project: %s/projects/%d
        
        Best regards,
        The Team
        """,
                projectTitle,
                frontendUrl,
                projectId
        );
    }
}
