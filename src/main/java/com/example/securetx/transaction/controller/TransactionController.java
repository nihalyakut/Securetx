package com.example.securetx.transaction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.securetx.transaction.dto.DepositRequest;
import com.example.securetx.transaction.dto.TransferRequest;
import com.example.securetx.transaction.service.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@PostMapping("/deposit")
	public ResponseEntity<Void> deposit(@Valid @RequestBody DepositRequest req) {
		transactionService.deposit(req.getIdempotencyKey(), req);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/transfer")
	public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest req) {
		transactionService.transfer(req.getIdempotencyKey(), req);
		return ResponseEntity.ok().build();
	}
}
