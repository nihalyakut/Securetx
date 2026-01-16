package com.example.securetx.transaction.dto;

import com.example.securetx.transaction.enums.TransactionStatus;

public final class TxCreateResult {

	private final Long transactionId;
	private final boolean existing;
	private final boolean conflict;
	private final TransactionStatus existingStatus;

	private TxCreateResult(Long transactionId, boolean existing, boolean conflict, TransactionStatus existingStatus) {
		this.transactionId = transactionId;
		this.existing = existing;
		this.conflict = conflict;
		this.existingStatus = existingStatus;
	}

	public static TxCreateResult created(Long transactionId) {
		return new TxCreateResult(transactionId, false, false, null);
	}

	public static TxCreateResult existing(Long transactionId, boolean conflict, TransactionStatus status) {
		return new TxCreateResult(transactionId, true, conflict, status);
	}

	public Long getTransactionId() {
		return transactionId;
	}

	public boolean isExisting() {
		return existing;
	}

	public boolean isConflict() {
		return conflict;
	}

	public TransactionStatus getExistingStatus() {
		return existingStatus;
	}
}
