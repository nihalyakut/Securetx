package com.example.securetx.ledger.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.securetx.ledger.enums.LedgerEntryType;

public class LedgerEntryResponse {

	private Long id;
	private Long transactionId;
	private LedgerEntryType entryType;
	private BigDecimal amount;
	private BigDecimal balanceAfter;
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(Long transactionId) {
		this.transactionId = transactionId;
	}

	public LedgerEntryType getEntryType() {
		return entryType;
	}

	public void setEntryType(LedgerEntryType entryType) {
		this.entryType = entryType;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getBalanceAfter() {
		return balanceAfter;
	}

	public void setBalanceAfter(BigDecimal balanceAfter) {
		this.balanceAfter = balanceAfter;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
