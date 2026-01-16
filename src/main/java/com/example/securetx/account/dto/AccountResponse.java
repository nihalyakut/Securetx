package com.example.securetx.account.dto;

import java.math.BigDecimal;

public class AccountResponse {

	private Long id;
	private String currency;
	private BigDecimal balance;
	private String status;

	public AccountResponse() {
	}

	public AccountResponse(Long id, String currency, BigDecimal balance, String status) {
		this.id = id;
		this.currency = currency;
		this.balance = balance;
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
