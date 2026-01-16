package com.example.securetx.ledger.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.securetx.ledger.dto.LedgerEntryResponse;
import com.example.securetx.ledger.service.LedgerService;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

	private final LedgerService ledgerService;

	public LedgerController(LedgerService ledgerService) {
		this.ledgerService = ledgerService;
	}

	@GetMapping("/mine")
	public ResponseEntity<List<LedgerEntryResponse>> myStatement(@RequestParam(name = "accountId") Long accountId,
			@RequestParam(name = "cursor", required = false) Long cursor,
			@RequestParam(name = "limit", required = false) Integer limit) {
		return ResponseEntity.ok(ledgerService.myStatement(accountId, cursor, limit));
	}
}
