package com.example.securetx.account.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.securetx.account.dto.AccountResponse;
import com.example.securetx.account.dto.CreateAccountRequest;
import com.example.securetx.account.entity.Account;
import com.example.securetx.account.enums.AccountStatus;
import com.example.securetx.account.repository.AccountRepository;
import com.example.securetx.audit.service.AuditService;
import com.example.securetx.auth.repository.UserRepository;

@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final AuditService auditService;

	public AccountService(AccountRepository accountRepository, UserRepository userRepository,
			AuditService auditService) {
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
		this.auditService = auditService;
	}

	@Transactional
	public AccountResponse createMyAccount(CreateAccountRequest req) {
		Long userId = currentUserId();

		Account a = new Account();
		a.setUserId(userId);
		a.setCurrency(req.getCurrency());
		a.setBalance(new BigDecimal("0.0000"));
		a.setStatus(AccountStatus.ACTIVE.name());
		Account saved = accountRepository.save(a);

		auditService.log(userId, "ACCOUNT_CREATE", "{\"accountId\":" + saved.getId() + "}");

		return new AccountResponse(saved.getId(), saved.getCurrency(), saved.getBalance(), saved.getStatus());
	}

	@Transactional(readOnly = true)
	public List<AccountResponse> listMyAccounts() {
		Long userId = currentUserId();
		return accountRepository.findByUserId(userId).stream()
				.map(a -> new AccountResponse(a.getId(), a.getCurrency(), a.getBalance(), a.getStatus())).toList();
	}

	private Long currentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {
			throw new IllegalStateException("Unauthenticated");
		}
		return userRepository.findByEmail(auth.getName()).orElseThrow(() -> new IllegalStateException("User not found")).getId();
	}
	
	@Transactional
	public void freeze(Long accountId) {
		Account acc = accountRepository.findByIdForUpdate(accountId)
				.orElseThrow(() -> new IllegalArgumentException("Account not found"));

		if (AccountStatus.FROZEN.name().equals(acc.getStatus())) {
			auditService.log(null, "ADMIN_ACCOUNT_FREEZE_NOOP", Map.of("accountId", accountId));
			throw new IllegalStateException("Account is already frozen");
		}

		acc.setStatus(AccountStatus.FROZEN.name());
		accountRepository.save(acc);

		auditService.log(null, "ADMIN_ACCOUNT_FROZEN", Map.of("accountId", accountId));
	}

	@Transactional
	public void unfreeze(Long accountId) {
		Account acc = accountRepository.findByIdForUpdate(accountId)
				.orElseThrow(() -> new IllegalArgumentException("Account not found"));

		if (AccountStatus.ACTIVE.name().equals(acc.getStatus())) {
			auditService.log(null, "ADMIN_ACCOUNT_UNFREEZE_NOOP", Map.of("accountId", accountId));
			 throw new IllegalStateException("Account is already unfrozen");
		}

		acc.setStatus(AccountStatus.ACTIVE.name());
		accountRepository.save(acc);

		auditService.log(null, "ADMIN_ACCOUNT_UNFROZEN", Map.of("accountId", accountId));
	}
}
