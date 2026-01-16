package com.example.securetx.transaction;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.securetx.account.entity.Account;
import com.example.securetx.account.enums.AccountStatus;
import com.example.securetx.account.repository.AccountRepository;
import com.example.securetx.audit.repository.AuditLogRepository;
import com.example.securetx.auth.entity.User;
import com.example.securetx.auth.repository.UserRepository;
import com.example.securetx.ledger.repository.LedgerEntryRepository;
import com.example.securetx.transaction.repository.TransactionRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc(addFilters = true)
class AccountFreezeAuthIT {

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
	}

	@Autowired
	MockMvc mockMvc;

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

	private User owner;
	private Account account;

	@BeforeEach
	void setup() {
		ledgerEntryRepository.deleteAll();
		auditLogRepository.deleteAll();
		transactionRepository.deleteAll();
		accountRepository.deleteAll();
		userRepository.deleteAll();

		owner = new User();
		owner.setEmail("owner@example.com");
		owner.setPasswordHash("dummy");
		owner.setRole("ROLE_USER");
		owner.setActive(true);
		owner.setCreatedAt(Instant.now());
		owner = userRepository.save(owner);

		account = new Account();
		account.setUserId(owner.getId());
		account.setCurrency("TRY");
		account.setBalance(new BigDecimal("1000.0000"));
		account.setStatus(AccountStatus.ACTIVE.name());
		account.setVersion(0L);
		account.setCreatedAt(Instant.now());
		account.setUpdatedAt(Instant.now());
		account = accountRepository.save(account);
	}

	private String bodyWithId(Long id) {
		return "{\"value\":" + id + "}";
	}

	@Test
	@WithMockUser(username = "user1@example.com", roles = "USER")
	void freeze_shouldReturn403_forNonAdmin() throws Exception {
		mockMvc.perform(post("/api/v1/accounts/freeze").contentType(MediaType.APPLICATION_JSON)
				.content(bodyWithId(account.getId()))).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "admin@example.com", roles = "ADMIN")
	void freeze_shouldReturn200_forAdmin_andUpdateStatus() throws Exception {
		mockMvc.perform(post("/api/v1/accounts/freeze").contentType(MediaType.APPLICATION_JSON)
				.content(bodyWithId(account.getId()))).andExpect(status().isOk());

		Account updated = accountRepository.findById(account.getId()).orElseThrow();
		Assertions.assertEquals(AccountStatus.FROZEN.name(), updated.getStatus());
	}

	@Test
	@WithMockUser(username = "admin@example.com", roles = "ADMIN")
	void unfreeze_shouldReturn200_forAdmin_andUpdateStatus() throws Exception {
		account.setStatus(AccountStatus.FROZEN.name());
		accountRepository.save(account);

		mockMvc.perform(post("/api/v1/accounts/unfreeze").contentType(MediaType.APPLICATION_JSON)
				.content(bodyWithId(account.getId()))).andExpect(status().isOk());

		Account updated = accountRepository.findById(account.getId()).orElseThrow();
		Assertions.assertEquals(AccountStatus.ACTIVE.name(), updated.getStatus());
	}

	@Test
	@WithMockUser(username = "admin@example.com", roles = "ADMIN")
	void freeze_shouldReturn400_whenBodyMissing() throws Exception {
		mockMvc.perform(post("/api/v1/accounts/freeze").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}
}
