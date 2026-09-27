package com.researchmate.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.researchmate.dto.request.ForgotPasswordRequest;
import com.researchmate.dto.request.ResetPasswordRequest;
import com.researchmate.dto.request.LoginRequest;
import com.researchmate.dto.request.RegisterRequest;
import com.researchmate.dto.response.LoginResponse;
import com.researchmate.dto.response.RegisterResponse;
import com.researchmate.entity.ResearchActivity;
import com.researchmate.entity.User;
import com.researchmate.exception.DuplicateEmailException;
import com.researchmate.exception.InvalidCredentialsException;
import com.researchmate.repository.ResearchActivityRepository;
import com.researchmate.repository.UserRepository;
import com.researchmate.repository.PasswordResetTokenRepository;
import com.researchmate.entity.PasswordResetToken;


@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ResearchActivityRepository activityRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
private final EmailService emailService;
@Value("${app.frontend-url}")
private String frontendUrl;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ResearchActivityRepository activityRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.activityRepository = activityRepository;
        this.passwordResetTokenRepository=passwordResetTokenRepository;
        this.emailService=emailService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Processing user registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateEmailException("Email '" + request.getEmail().trim() + "' is already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");

        User savedUser = userRepository.save(user);

        activityRepository.save(new ResearchActivity(
                savedUser,
                "REGISTRATION",
                "Created student account with email: " + savedUser.getEmail()
        ));

        log.info("User successfully registered with ID: {}", savedUser.getId());

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Processing login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        activityRepository.save(new ResearchActivity(
                user,
                "LOGIN",
                "User logged in successfully"
        ));

        log.info("User {} logged in successfully", user.getId());

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }

@Transactional
public void forgotPassword(ForgotPasswordRequest request) {

    String email = request.getEmail()
            .trim()
            .toLowerCase();

    /*
     * We intentionally do not reveal whether the email exists.
     * This prevents account/email enumeration.
     */
    userRepository.findByEmail(email).ifPresent(user -> {

        // Remove any previous reset token for this user.
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String token = java.util.UUID.randomUUID().toString();

        Instant expiresAt =
                Instant.now().plusSeconds(15 * 60);

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        token,
                        user,
                        expiresAt
                );

        passwordResetTokenRepository.save(resetToken);

        String resetLink =
                frontendUrl
                + "/reset-password?token="
                + token;

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                resetLink
        );

        log.info(
                "Password reset email requested for user ID: {}",
                user.getId()
        );
    });
}

@Transactional
public void resetPassword(ResetPasswordRequest request) {

    PasswordResetToken resetToken =
            passwordResetTokenRepository
                    .findByToken(request.getToken())
                    .orElseThrow(() ->
                            new InvalidCredentialsException(
                                    "Invalid or expired reset token"
                            ));

    if (resetToken.isUsed()) {
        throw new InvalidCredentialsException(
                "Invalid or expired reset token"
        );
    }

    if (resetToken.getExpiresAt().isBefore(Instant.now())) {
        throw new InvalidCredentialsException(
                "Invalid or expired reset token"
        );
    }

    User user = resetToken.getUser();

    user.setPassword(
            passwordEncoder.encode(request.getPassword())
    );

    userRepository.save(user);

    resetToken.setUsed(true);
    passwordResetTokenRepository.save(resetToken);

    activityRepository.save(
            new ResearchActivity(
                    user,
                    "PASSWORD_RESET",
                    "User successfully reset their password"
            )
    );

    log.info(
            "Password successfully reset for user ID: {}",
            user.getId()
    );
}

}
