package com.examlple.projectservice.service;

import com.examlple.projectservice.entity.Invitation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    public void sendInvitationEmail(Invitation invitation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(invitation.getEmail());
        message.setSubject("Project Invitation");
        message.setText(
                "You have been invited to join a project.\n\n" +
                        "Click here to accept: " + frontendUrl + "/invitations/accept?token=" + invitation.getToken() + "\n\n" +
                        "This invitation expires in 7 days."
        );

        mailSender.send(message);
    }

    @Async
    public void sendDirectAddEmail(String email, Long projectId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Added to Project");
        message.setText(
                "You have been added to a project.\n\n" +
                        "View project: " + frontendUrl + "/projects/" + projectId
        );

        mailSender.send(message);
    }
}
