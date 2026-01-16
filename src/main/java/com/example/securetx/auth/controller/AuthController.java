package com.example.securetx.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.securetx.auth.dto.AuthLoginRequest;
import com.example.securetx.auth.dto.AuthRegisterRequest;
import com.example.securetx.auth.dto.AuthResponse;
import com.example.securetx.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<Void> register(@Valid @RequestBody AuthRegisterRequest request) {
		authService.register(request);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}
}
