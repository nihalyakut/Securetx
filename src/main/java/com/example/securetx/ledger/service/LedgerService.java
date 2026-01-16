package com.example.securetx.ledger.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.securetx.account.entity.Account;
import com.example.securetx.account.repository.AccountRepository;
import com.example.securetx.auth.repository.UserRepository;
import com.example.securetx.ledger.dto.LedgerEntryResponse;
import com.example.securetx.ledger.entity.LedgerEntry;
import com.example.securetx.ledger.repository.LedgerEntryRepository;

@Service
public class LedgerService {

	private final LedgerEntryRepository ledgerEntryRepository;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;

	public LedgerService(LedgerEntryRepository ledgerEntryRepository, AccountRepository accountRepository,
			UserRepository userRepository) {
		this.ledgerEntryRepository = ledgerEntryRepository;
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
	}

	public List<LedgerEntryResponse> myStatement(Long accountId, Long cursor, Integer limit) {
		Long userId = currentUserId();

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new IllegalArgumentException("Account not found"));

		if (!account.getUserId().equals(userId)) {
			throw new SecurityException("You cannot view statement of an account you do not own");
		}

		int size = (limit == null) ? 50 : Math.min(Math.max(limit, 1), 200);

		List<LedgerEntry> rows = ledgerEntryRepository.findStatement(accountId, cursor, PageRequest.of(0, size));

		return rows.stream().map(l -> {
			LedgerEntryResponse dto = new LedgerEntryResponse();
			dto.setId(l.getId());
			dto.setTransactionId(l.getTransactionId());
			dto.setEntryType(l.getEntryType());
			dto.setAmount(l.getAmount());
			dto.setBalanceAfter(l.getBalanceAfter());
			dto.setCreatedAt(l.getCreatedAt());
			return dto;
		}).toList();
	}

	private Long currentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null)
			throw new IllegalStateException("Unauthenticated");
		return userRepository.findByEmail(auth.getName()).orElseThrow(() -> new IllegalStateException("User not found"))
				.getId();
	}
}
