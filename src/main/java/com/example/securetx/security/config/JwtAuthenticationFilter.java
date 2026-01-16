package com.example.securetx.security.config;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.securetx.security.service.CustomUserDetailsService;
import com.example.securetx.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService, ObjectMapper objectMapper) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring("Bearer ".length()).trim();
		try {
			String email = jwtService.extractSubject(token);

			if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(email);

				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());

				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}

		} catch (JwtException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, ex.getMessage());
		}

		filterChain.doFilter(request, response);
	}
	
    private void writeUnauthorizedResponse(HttpServletResponse response, String detail)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = Map.of(
                "error", "UNAUTHORIZED",
                "message", "Invalid or expired JWT token"
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
