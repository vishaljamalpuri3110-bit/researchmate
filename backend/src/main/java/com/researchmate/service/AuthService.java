package com.researchmate.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.researchmate.dto.request.RegisterRequest;
import com.researchmate.dto.response.RegisterResponse;
import com.researchmate.entity.User;
import com.researchmate.repository.UserRepository;
import com.researchmate.dto.request.LoginRequest;
import com.researchmate.dto.response.LoginResponse;

import jakarta.transaction.Transactional;
@Service 
public class AuthService {
    private  final UserRepository userRepository;
    private  final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    public AuthService(UserRepository userRepository,PasswordEncoder passwordEncoder,JwtService jwtService){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtService=jwtService;
    }

    @Transactional 
    public RegisterResponse register(RegisterRequest request){

        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email already registered.");
        }

        User user=new User();

        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole("STUDENT");

        User saveUser=userRepository.save(user);

        return new RegisterResponse(
            saveUser.getId(),
            saveUser.getEmail(),
            saveUser.getPassword()
        );
    }

    public LoginResponse login(LoginRequest request) {

    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() ->
                    new RuntimeException("Invalid email or password"));

    if (!passwordEncoder.matches(
            request.getPassword(),
            user.getPassword())) {

        throw new RuntimeException("Invalid email or password");
    }

    String token = jwtService.generateToken(user);

    return new LoginResponse(
            token,
            "Bearer",
            user.getId(),
            user.getEmail(),
            user.getRole()
    );
}

}
