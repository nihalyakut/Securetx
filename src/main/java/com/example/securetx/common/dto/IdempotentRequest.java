package com.example.securetx.common.dto;

import jakarta.validation.constraints.NotBlank;

public abstract class IdempotentRequest {
	
	@NotBlank
	private String idempotencyKey;

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}

}
