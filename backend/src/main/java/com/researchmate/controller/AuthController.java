package com.researchmate.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.researchmate.dto.request.ForgotPasswordRequest;
import com.researchmate.dto.request.ResetPasswordRequest;
import com.researchmate.dto.request.LoginRequest;
import com.researchmate.dto.request.RegisterRequest;
import com.researchmate.dto.response.LoginResponse;
import com.researchmate.dto.response.RegisterResponse;
import com.researchmate.service.AuthService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService=authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest){
        RegisterResponse response=authService.register(registerRequest);

        return ResponseEntity
               .status(HttpStatus.CREATED)
               .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request) {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
    }

@PostMapping("/forgot-password")
public ResponseEntity<String> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request) {

    authService.forgotPassword(request);

    return ResponseEntity.ok(
            "If the email is registered, a password reset link has been sent."
    );
}

@PostMapping("/reset-password")
public ResponseEntity<String> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request) {

    authService.resetPassword(request);

    return ResponseEntity.ok(
            "Password reset successfully."
    );
}

}
