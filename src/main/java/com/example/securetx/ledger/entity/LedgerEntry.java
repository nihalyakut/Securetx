package com.example.securetx.ledger.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

import com.example.securetx.ledger.enums.LedgerEntryType;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "transaction_id", nullable = false)
	private Long transactionId;

	@Column(name = "account_id", nullable = false)
	private Long accountId;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false, length = 10)
	private LedgerEntryType entryType;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
	private BigDecimal balanceAfter;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

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

	public Long getAccountId() {
		return accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
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
