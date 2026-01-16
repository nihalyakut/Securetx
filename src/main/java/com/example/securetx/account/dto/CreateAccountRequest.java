package com.example.securetx.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateAccountRequest {

	@NotBlank
	@Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter code like TRY, USD")
	private String currency;

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

}
