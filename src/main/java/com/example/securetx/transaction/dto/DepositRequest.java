package com.example.securetx.transaction.dto;

import java.math.BigDecimal;

import com.example.securetx.common.dto.IdempotentRequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class DepositRequest extends IdempotentRequest {

    @NotNull
    private Long accountId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency;

	public Long getAccountId() {
		return accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
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
