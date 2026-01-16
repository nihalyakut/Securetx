package com.example.securetx.common.dto;

import java.time.Instant;
import java.util.List;

public class ApiErrorResponse {

	private Instant timestamp;
	private int status;
	private String error;
	private String message;
	private String path;
	private List<FieldViolation> violations;

	public ApiErrorResponse(Instant timestamp, int status, String error, String message, String path, List<FieldViolation> violations) {
		this.timestamp = timestamp;
		this.status = status;
		this.error = error;
		this.message = message;
		this.path = path;
		this.violations = violations;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public List<FieldViolation> getViolations() {
		return violations;
	}

	public void setViolations(List<FieldViolation> violations) {
		this.violations = violations;
	}

	public static class FieldViolation {
		private String field;
		private String message;

		public FieldViolation() {
		}

		public FieldViolation(String field, String message) {
			this.field = field;
			this.message = message;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
