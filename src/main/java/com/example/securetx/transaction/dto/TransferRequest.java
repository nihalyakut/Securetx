package com.example.securetx.transaction.dto;

import java.math.BigDecimal;

import com.example.securetx.common.dto.IdempotentRequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class TransferRequest extends IdempotentRequest {

	@NotNull
	private Long fromAccountId;

	@NotNull
	private Long toAccountId;

	@NotNull
	@Positive
	private BigDecimal amount;

	@NotNull
	@Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter code like TRY, USD")
	private String currency;

	public Long getFromAccountId() {
		return fromAccountId;
	}

	public void setFromAccountId(Long fromAccountId) {
		this.fromAccountId = fromAccountId;
	}

	public Long getToAccountId() {
		return toAccountId;
	}

	public void setToAccountId(Long toAccountId) {
		this.toAccountId = toAccountId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

}
