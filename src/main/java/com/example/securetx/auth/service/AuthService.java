package com.example.securetx.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.securetx.auth.dto.AuthLoginRequest;
import com.example.securetx.auth.dto.AuthRegisterRequest;
import com.example.securetx.auth.dto.AuthResponse;
import com.example.securetx.auth.entity.User;
import com.example.securetx.auth.repository.UserRepository;
import com.example.securetx.security.service.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public void register(AuthRegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User u = new User();
        u.setEmail(req.getEmail());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRole("ROLE_USER");
        u.setActive(true);

        userRepository.save(u);
    }

    public AuthResponse login(AuthLoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found after auth"));

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, jwtService.getExpirationSeconds());
    }
}
