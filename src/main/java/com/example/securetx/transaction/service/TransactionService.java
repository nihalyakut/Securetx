package com.example.securetx.transaction.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.securetx.account.entity.Account;
import com.example.securetx.account.enums.AccountStatus;
import com.example.securetx.account.repository.AccountRepository;
import com.example.securetx.audit.service.AuditService;
import com.example.securetx.auth.repository.UserRepository;
import com.example.securetx.common.util.HashUtil;
import com.example.securetx.common.util.MoneyUtil;
import com.example.securetx.ledger.entity.LedgerEntry;
import com.example.securetx.ledger.enums.LedgerEntryType;
import com.example.securetx.ledger.repository.LedgerEntryRepository;
import com.example.securetx.transaction.dto.DepositRequest;
import com.example.securetx.transaction.dto.TransferRequest;
import com.example.securetx.transaction.dto.TxCreateResult;
import com.example.securetx.transaction.entity.Transaction;
import com.example.securetx.transaction.enums.TransactionStatus;
import com.example.securetx.transaction.repository.TransactionRepository;

@Service
public class TransactionService {

	private final TransactionRepository transactionRepository;
	private final TransactionIdempotencyService transactionIdempotencyService;
	private final AccountRepository accountRepository;
	private final LedgerEntryRepository ledgerEntryRepository;
	private final UserRepository userRepository;
	private final AuditService auditService;

	public TransactionService(TransactionRepository transactionRepository,
			TransactionIdempotencyService transactionIdempotencyService, AccountRepository accountRepository,
			LedgerEntryRepository ledgerEntryRepository, UserRepository userRepository, AuditService auditService) {
		this.transactionRepository = transactionRepository;
		this.transactionIdempotencyService = transactionIdempotencyService;
		this.accountRepository = accountRepository;
		this.ledgerEntryRepository = ledgerEntryRepository;
		this.userRepository = userRepository;
		this.auditService = auditService;
	}

	@Transactional
	public void deposit(String idempotencyKey, DepositRequest req) {

		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new IllegalArgumentException("Idempotency-Key header is required");
		}

		Long userId = currentUserId();
		BigDecimal amount = MoneyUtil.scale(req.getAmount());

		String canonical = "DEPOSIT|" + req.getAccountId() + "|" + amount.toPlainString() + "|" + req.getCurrency();
		String requestHash = HashUtil.sha256Hex(canonical);

		TxCreateResult txResult = transactionIdempotencyService.getOrCreateDepositTx(idempotencyKey, req, amount,
				requestHash);

		if (txResult.isConflict()) {
			auditService.log(userId, "DEPOSIT_IDEMPOTENCY_CONFLICT", Map.of("key", idempotencyKey, "existingTxId",
					txResult.getTransactionId(), "accountId", req.getAccountId()));
			throw new IllegalStateException("Idempotency-Key already used with different request");
		}

		if (txResult.isExisting()) {
			TransactionStatus st = txResult.getExistingStatus();
			if (st == TransactionStatus.COMPLETED) {
				auditService.log(userId, "DEPOSIT_IDEMPOTENT_HIT", Map.of("key", idempotencyKey, "existingTxId",
						txResult.getTransactionId(), "accountId", req.getAccountId()));
				return;
			}
			if (st == TransactionStatus.PENDING) {
				throw new IllegalStateException("Deposit request is already being processed");
			}
			if (st == TransactionStatus.FAILED) {
				throw new IllegalStateException("Deposit with this Idempotency-Key already failed");
			}
			throw new IllegalStateException("Unexpected transaction state for idempotency key");
		}

		Transaction tx = transactionRepository.findById(txResult.getTransactionId())
				.orElseThrow(() -> new IllegalStateException("Transaction not found"));

		try {
			Account account = accountRepository.findByIdForUpdate(req.getAccountId())
					.orElseThrow(() -> new IllegalArgumentException("Account not found"));

			if (!account.getUserId().equals(userId))
				throw new SecurityException("You cannot deposit into an account you do not own");
			if (!AccountStatus.ACTIVE.name().equals(account.getStatus()))
				throw new IllegalStateException("Account is not ACTIVE");
			if (!req.getCurrency().equals(account.getCurrency()))
				throw new IllegalArgumentException("Currency mismatch");

			BigDecimal after = MoneyUtil.scale(account.getBalance().add(amount));
			account.setBalance(after);
			accountRepository.save(account);

			LedgerEntry credit = new LedgerEntry();
			credit.setTransactionId(tx.getId());
			credit.setAccountId(account.getId());
			credit.setEntryType(LedgerEntryType.CREDIT);
			credit.setAmount(amount);
			credit.setBalanceAfter(after);
			credit.setCreatedAt(Instant.now());
			ledgerEntryRepository.save(credit);

			tx.setStatus(TransactionStatus.COMPLETED);
			transactionRepository.save(tx);

			auditService.log(userId, "DEPOSIT_COMPLETED", Map.of("txId", tx.getId(), "accountId", account.getId(),
					"amount", amount, "currency", req.getCurrency()));

		} catch (RuntimeException ex) {
			try {
				tx.setStatus(TransactionStatus.FAILED);
				tx.setFailureReason(toFailureReason(ex));
				transactionRepository.save(tx);
			} catch (Exception ignore) {
			}

			auditService.log(userId, "DEPOSIT_FAILED",
					Map.of("txId", tx.getId(), "accountId", req.getAccountId(), "reason", toFailureReason(ex)));
			throw ex;
		}
	}

	@Transactional
	public void transfer(String idempotencyKey, TransferRequest req) {

		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new IllegalArgumentException("Idempotency-Key header is required");
		}
		if (req.getFromAccountId().equals(req.getToAccountId())) {
			throw new IllegalArgumentException("fromAccountId and toAccountId cannot be the same");
		}

		Long userId = currentUserId();
		BigDecimal amount = MoneyUtil.scale(req.getAmount());

		String canonical = "TRANSFER|" + req.getFromAccountId() + "|" + req.getToAccountId() + "|"
				+ amount.toPlainString() + "|" + req.getCurrency();
		String requestHash = HashUtil.sha256Hex(canonical);

		TxCreateResult txResult = transactionIdempotencyService.getOrCreateTransferTx(idempotencyKey, req, amount,
				requestHash);

		if (txResult.isConflict()) {
			auditService.log(userId, "TRANSFER_IDEMPOTENCY_CONFLICT",
					Map.of("key", idempotencyKey, "existingTxId", txResult.getTransactionId()));
			throw new IllegalStateException("Idempotency-Key already used with different request");
		}

		if (txResult.isExisting()) {
			TransactionStatus st = txResult.getExistingStatus();
			if (st == TransactionStatus.COMPLETED) {
				auditService.log(userId, "TRANSFER_IDEMPOTENT_HIT",
						Map.of("key", idempotencyKey, "existingTxId", txResult.getTransactionId()));
				return;
			}
			if (st == TransactionStatus.PENDING) {
				throw new IllegalStateException("Transfer request is already being processed");
			}
			if (st == TransactionStatus.FAILED) {
				throw new IllegalStateException("Transfer with this Idempotency-Key already failed");
			}
			throw new IllegalStateException("Unexpected transaction state for idempotency key");
		}

		Transaction tx = transactionRepository.findById(txResult.getTransactionId())
				.orElseThrow(() -> new IllegalStateException("Transaction not found"));

		// Deadlock-safe lock order
		Long firstId = Math.min(req.getFromAccountId(), req.getToAccountId());
		Long secondId = Math.max(req.getFromAccountId(), req.getToAccountId());

		try {
			Account first = accountRepository.findByIdForUpdate(firstId)
					.orElseThrow(() -> new IllegalArgumentException("Account not found: " + firstId));

			Account second = accountRepository.findByIdForUpdate(secondId)
					.orElseThrow(() -> new IllegalArgumentException("Account not found: " + secondId));

			Account from = first.getId().equals(req.getFromAccountId()) ? first : second;
			Account to = first.getId().equals(req.getToAccountId()) ? first : second;

			// ownership: must own from account
			if (!from.getUserId().equals(userId)) {
				throw new SecurityException("You cannot transfer from an account you do not own");
			}

			if (!AccountStatus.ACTIVE.name().equals(from.getStatus())) {
				throw new IllegalStateException("From account is not ACTIVE");
			}
			if (!AccountStatus.ACTIVE.name().equals(to.getStatus())) {
				throw new IllegalStateException("To account is not ACTIVE");
			}

			if (!req.getCurrency().equals(from.getCurrency()) || !req.getCurrency().equals(to.getCurrency())) {
				throw new IllegalArgumentException("Currency mismatch");
			}

			if (from.getBalance().compareTo(amount) < 0) {
				throw new IllegalStateException("Insufficient funds");
			}

			BigDecimal fromAfter = MoneyUtil.scale(from.getBalance().subtract(amount));
			BigDecimal toAfter = MoneyUtil.scale(to.getBalance().add(amount));

			from.setBalance(fromAfter);
			to.setBalance(toAfter);

			accountRepository.save(from);
			accountRepository.save(to);

			LedgerEntry debit = new LedgerEntry();
			debit.setTransactionId(tx.getId());
			debit.setAccountId(from.getId());
			debit.setEntryType(LedgerEntryType.DEBIT);
			debit.setAmount(amount);
			debit.setBalanceAfter(fromAfter);
			debit.setCreatedAt(Instant.now());
			ledgerEntryRepository.save(debit);

			LedgerEntry credit = new LedgerEntry();
			credit.setTransactionId(tx.getId());
			credit.setAccountId(to.getId());
			credit.setEntryType(LedgerEntryType.CREDIT);
			credit.setAmount(amount);
			credit.setBalanceAfter(toAfter);
			credit.setCreatedAt(Instant.now());
			ledgerEntryRepository.save(credit);

			tx.setStatus(TransactionStatus.COMPLETED);
			transactionRepository.save(tx);

			auditService.log(userId, "TRANSFER_COMPLETED", Map.of("txId", tx.getId(), "fromAccountId", from.getId(),
					"toAccountId", to.getId(), "amount", amount, "currency", req.getCurrency()));

		} catch (RuntimeException ex) {
			transactionIdempotencyService.markFailed(tx.getId(), toFailureReason(ex));

			auditService.log(userId, "TRANSFER_FAILED", Map.of("txId", tx.getId(), "fromAccountId",
					req.getFromAccountId(), "toAccountId", req.getToAccountId(), "reason", toFailureReason(ex)));

			throw ex;
		}
	}

	
	private String toFailureReason(RuntimeException ex) {
		if (ex instanceof IllegalArgumentException)
			return "INVALID_REQUEST";
		if (ex instanceof SecurityException)
			return "FORBIDDEN_ACCOUNT";
		if (ex instanceof IllegalStateException)
			return "BUSINESS_RULE";
		return "UNEXPECTED_ERROR";
	}

	private Long currentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {
			throw new IllegalStateException("Unauthenticated");
		}
		return userRepository.findByEmail(auth.getName()).orElseThrow(() -> new IllegalStateException("User not found"))
				.getId();
	}
}
