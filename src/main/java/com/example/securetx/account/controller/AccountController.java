package com.example.securetx.account.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.securetx.account.dto.AccountResponse;
import com.example.securetx.account.dto.CreateAccountRequest;
import com.example.securetx.account.service.AccountService;
import com.example.securetx.common.dto.LongParameter;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@PostMapping
	public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest req) {
		return ResponseEntity.ok(accountService.createMyAccount(req));
	}

	@GetMapping("/mine")
	public ResponseEntity<List<AccountResponse>> mine() {
		return ResponseEntity.ok(accountService.listMyAccounts());
	}
	
	@PostMapping("/freeze")
	public ResponseEntity<Void> freeze(@Valid @RequestBody LongParameter param) {
		accountService.freeze(param.getValue());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/unfreeze")
	public ResponseEntity<Void> unfreeze(@Valid @RequestBody LongParameter param) {
		accountService.unfreeze(param.getValue());
		return ResponseEntity.ok().build();
	}
}
