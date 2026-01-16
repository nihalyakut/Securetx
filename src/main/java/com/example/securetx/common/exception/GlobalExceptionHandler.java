package com.example.securetx.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import com.example.securetx.common.dto.ApiErrorResponse;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		List<ApiErrorResponse.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ApiErrorResponse.FieldViolation(fe.getField(), fe.getDefaultMessage())).toList();

		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
				"Request validation failed", request.getRequestURI(), violations);

		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
			HttpServletRequest request) {
		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST",
				safeMessage(ex.getMessage(), "Invalid request"), request.getRequestURI(), null);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), HttpStatus.CONFLICT.value(), "CONFLICT",
				safeMessage(ex.getMessage(), "Request could not be processed due to a conflict"),
				request.getRequestURI(), null);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	@ExceptionHandler(SecurityException.class)
	public ResponseEntity<ApiErrorResponse> handleSecurity(SecurityException ex, HttpServletRequest request) {
		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), HttpStatus.FORBIDDEN.value(), "FORBIDDEN",
				safeMessage(ex.getMessage(), "Forbidden"), request.getRequestURI(), null);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ApiErrorResponse> handleDatabase(DataAccessException ex, HttpServletRequest request) {
		log.error("Database error", ex);

		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"DATABASE_ERROR", "Unexpected database error occurred", request.getRequestURI(), null);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception", ex);

		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"INTERNAL_ERROR", "Unexpected system error occurred. Please try again later.", request.getRequestURI(),
				null);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}

	private String safeMessage(String candidate, String fallback) {
		return (candidate != null && !candidate.isBlank()) ? candidate : fallback;
	}
}
