package com.example.securetx.security.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	
	private final Key signingKey;
	private final long expirationSeconds;

	public JwtService(@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration-seconds}") long expirationSeconds) {
		if (secret == null || secret.length() < 32) {
			throw new IllegalArgumentException("jwt.secret must be at least 32 characters");
		}
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationSeconds = expirationSeconds;
	}

	public long getExpirationSeconds() {
		return expirationSeconds;
	}

	public String generateToken(String subjectEmail, String role) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(expirationSeconds);

		return Jwts.builder().setSubject(subjectEmail).setIssuedAt(Date.from(now)).setExpiration(Date.from(exp))
				.addClaims(Map.of("role", role)).signWith(signingKey, SignatureAlgorithm.HS256).compact();
	}

	public Jws<Claims> parse(String token) throws JwtException {
		return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
	}

	public String extractSubject(String token) {
		return parse(token).getBody().getSubject();
	}

	public String extractRole(String token) {
		Object role = parse(token).getBody().get("role");
		return role == null ? null : role.toString();
	}
}
