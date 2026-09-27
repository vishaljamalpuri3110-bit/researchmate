package com.researchmate.service;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.researchmate.exception.EmailServiceException;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    public void sendPasswordResetEmail(String email, String resetLink) {

    try {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("ResearchMate - Password Reset");

        message.setText(
            "Hello,\n\n"
            + "We received a request to reset your ResearchMate password.\n\n"
            + "Use this link to reset your password:\n\n"
            + resetLink
            + "\n\n"
            + "This link will expire in 15 minutes.\n\n"
            + "Regards,\n"
            + "ResearchMate"
        );

        mailSender.send(message);

    } catch (MailAuthenticationException ex) {

        log.error("SMTP authentication failed while sending reset email", ex);

        throw new EmailServiceException(
            "Unable to send password reset email.",
            ex
        );

    } catch (MailException ex) {

        log.error("Email sending failed", ex);

        throw new EmailServiceException(
            "Unable to send password reset email.",
            ex
        );
    }
}
}
