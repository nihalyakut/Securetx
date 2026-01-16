package com.example.securetx.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.securetx.account.entity.Account;
import com.example.securetx.account.enums.AccountStatus;
import com.example.securetx.account.repository.AccountRepository;
import com.example.securetx.account.service.AccountService;
import com.example.securetx.audit.repository.AuditLogRepository;
import com.example.securetx.auth.entity.User;
import com.example.securetx.auth.repository.UserRepository;
import com.example.securetx.ledger.repository.LedgerEntryRepository;
import com.example.securetx.transaction.dto.TransferRequest;
import com.example.securetx.transaction.enums.TransactionStatus;
import com.example.securetx.transaction.repository.TransactionRepository;
import com.example.securetx.transaction.service.TransactionService;

@SpringBootTest
@Testcontainers
class TransferConcurrencyIT {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
			.withDatabaseName("securetx_db").withUsername("securetx_user").withPassword("securetx_pass")
			.withInitScript("schema.sql");

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry r) {
		r.add("spring.datasource.url", postgres::getJdbcUrl);
		r.add("spring.datasource.username", postgres::getUsername);
		r.add("spring.datasource.password", postgres::getPassword);

		r.add("spring.jpa.properties.hibernate.default_schema", () -> "securetx");
		r.add("spring.jpa.hibernate.ddl-auto", () -> "none");

		r.add("spring.datasource.hikari.maximum-pool-size", () -> "50");
		r.add("spring.datasource.hikari.connection-timeout", () -> "30000");
	}

	@Autowired
	UserRepository userRepository;
	@Autowired
	AccountRepository accountRepository;
	@Autowired
	TransactionRepository transactionRepository;
	@Autowired
	LedgerEntryRepository ledgerEntryRepository;
	@Autowired
	AuditLogRepository auditLogRepository;

	@Autowired
	TransactionService transactionService;
	@Autowired
	AccountService accountService;

	private User userA;
	private Account accA;
	private Account accB;

	@BeforeEach
	void setup() {
		ledgerEntryRepository.deleteAll();
		auditLogRepository.deleteAll();
		transactionRepository.deleteAll();
		accountRepository.deleteAll();
		userRepository.deleteAll();

		userA = new User();
		userA.setEmail("a@example.com");
		userA.setPasswordHash("dummy");
		userA.setRole("ROLE_USER");
		userA.setActive(true);
		userA.setCreatedAt(Instant.now());
		userA = userRepository.save(userA);

		accA = new Account();
		accA.setUserId(userA.getId());
		accA.setCurrency("TRY");
		accA.setBalance(new BigDecimal("1000.0000"));
		accA.setStatus(AccountStatus.ACTIVE.name());
		accA.setVersion(0L);
		accA.setCreatedAt(Instant.now());
		accA.setUpdatedAt(Instant.now());
		accA = accountRepository.save(accA);

		accB = new Account();
		accB.setUserId(userA.getId());
		accB.setCurrency("TRY");
		accB.setBalance(new BigDecimal("0.0000"));
		accB.setStatus(AccountStatus.ACTIVE.name());
		accB.setVersion(0L);
		accB.setCreatedAt(Instant.now());
		accB.setUpdatedAt(Instant.now());
		accB = accountRepository.save(accB);
	}

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	enum ResultCode {
		OK, INSUFFICIENT_FUNDS, NOT_ACTIVE, FORBIDDEN, VALIDATION, IDEMPOTENCY_CONFLICT, OTHER
	}

	@Test
	void shouldPreventDoubleSpend_underConcurrentTransfers() throws Exception {
		int threads = 20;
		BigDecimal amount = new BigDecimal("100.0000");
		int expectedSuccess = 10;

		ExecutorService pool = Executors.newFixedThreadPool(threads);

		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);

		List<Future<ResultCode>> futures = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			futures.add(pool.submit(() -> {
				ready.countDown();

				if (!start.await(10, TimeUnit.SECONDS)) {
					done.countDown();
					return ResultCode.OTHER;
				}

				setAuthEmail(userA.getEmail());

				try {
					TransferRequest req = new TransferRequest();
					req.setFromAccountId(accA.getId());
					req.setToAccountId(accB.getId());
					req.setAmount(amount);
					req.setCurrency("TRY");

					String idemKey = "trf-" + UUID.randomUUID();
					transactionService.transfer(idemKey, req);
					return ResultCode.OK;

				} catch (Exception e) {
					return classify(e);
				} finally {
					SecurityContextHolder.clearContext();
					done.countDown();
				}
			}));
		}

		Assertions.assertTrue(ready.await(10, TimeUnit.SECONDS));
		start.countDown();
		Assertions.assertTrue(done.await(60, TimeUnit.SECONDS));

		pool.shutdown();
		Assertions.assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));

		// aggregate results
		Map<ResultCode, AtomicInteger> counts = new ConcurrentHashMap<>();
		for (ResultCode c : ResultCode.values())
			counts.put(c, new AtomicInteger(0));

		for (Future<ResultCode> f : futures) {
			ResultCode c = f.get();
			counts.get(c).incrementAndGet();
		}

		int ok = counts.get(ResultCode.OK).get();
		Assertions.assertEquals(expectedSuccess, ok, "OK count mismatch. See breakdown above.");
		Assertions.assertEquals(threads - expectedSuccess, threads - ok);

		Account aFinal = accountRepository.findById(accA.getId()).orElseThrow();
		Account bFinal = accountRepository.findById(accB.getId()).orElseThrow();

		Assertions.assertTrue(aFinal.getBalance().compareTo(BigDecimal.ZERO) >= 0);
		Assertions.assertEquals(new BigDecimal("1000.0000"), aFinal.getBalance().add(bFinal.getBalance()));

		var allTx = transactionRepository.findAll();

		long completed = allTx.stream().filter(t -> TransactionStatus.COMPLETED.equals(t.getStatus())).count();

		long failed = allTx.stream().filter(t -> TransactionStatus.FAILED.equals(t.getStatus())).count();

		Assertions.assertEquals(expectedSuccess, completed, "COMPLETED count mismatch (mapping issue olabilir)");
		Assertions.assertEquals(threads - expectedSuccess, failed, "FAILED count mismatch (mapping issue olabilir)");
	}

	@Test
	void shouldBlockTransfers_whenAccountFrozen_thenAllowAfterUnfreeze() {
		accountService.freeze(accA.getId());

		Account frozen = accountRepository.findById(accA.getId()).orElseThrow();
		Assertions.assertEquals(AccountStatus.FROZEN.name(), frozen.getStatus());

		setAuthEmail(userA.getEmail());

		TransferRequest req = new TransferRequest();
		req.setFromAccountId(accA.getId());
		req.setToAccountId(accB.getId());
		req.setAmount(new BigDecimal("10.0000"));
		req.setCurrency("TRY");

		String idemKey1 = "frz-" + UUID.randomUUID();

		Exception ex = Assertions.assertThrows(Exception.class, () -> transactionService.transfer(idemKey1, req));
		String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
		Assertions.assertTrue(msg.contains("active") || msg.contains("frozen") || msg.contains("not"),
				"Expected a status-related message but was: " + ex.getMessage());

		SecurityContextHolder.clearContext();

		accountService.unfreeze(accA.getId());
		Account activeAgain = accountRepository.findById(accA.getId()).orElseThrow();
		Assertions.assertEquals(AccountStatus.ACTIVE.name(), activeAgain.getStatus());

		setAuthEmail(userA.getEmail());
		String idemKey2 = "unfrz-" + UUID.randomUUID();

		Assertions.assertDoesNotThrow(() -> transactionService.transfer(idemKey2, req));
		SecurityContextHolder.clearContext();

		Account aFinal = accountRepository.findById(accA.getId()).orElseThrow();
		Account bFinal = accountRepository.findById(accB.getId()).orElseThrow();

		Assertions.assertEquals(new BigDecimal("990.0000"), aFinal.getBalance());
		Assertions.assertEquals(new BigDecimal("10.0000"), bFinal.getBalance());
	}

	private void setAuthEmail(String email) {
		var auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private ResultCode classify(Exception e) {
		String msg = messageDeep(e).toLowerCase();

		if (e instanceof SecurityException || msg.contains("forbidden") || msg.contains("do not own")) {
			return ResultCode.FORBIDDEN;
		}
		if (msg.contains("not active") || msg.contains("frozen")) {
			return ResultCode.NOT_ACTIVE;
		}
		if (msg.contains("insufficient") || msg.contains("balance")) {
			return ResultCode.INSUFFICIENT_FUNDS;
		}
		if (msg.contains("idempotency") && msg.contains("different")) {
			return ResultCode.IDEMPOTENCY_CONFLICT;
		}
		if (msg.contains("currency") || msg.contains("mismatch") || msg.contains("invalid")) {
			return ResultCode.VALIDATION;
		}
		return ResultCode.OTHER;
	}

	private String messageDeep(Throwable t) {
		Throwable cur = t;
		String lastMsg = "";
		while (cur != null) {
			if (cur.getMessage() != null && !cur.getMessage().isBlank()) {
				lastMsg = cur.getMessage();
			}
			cur = cur.getCause();
		}
		return lastMsg == null ? "" : lastMsg;
	}
}
