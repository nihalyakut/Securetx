package com.example.securetx.transaction.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.securetx.transaction.dto.DepositRequest;
import com.example.securetx.transaction.dto.TransferRequest;
import com.example.securetx.transaction.dto.TxCreateResult;
import com.example.securetx.transaction.entity.Transaction;
import com.example.securetx.transaction.enums.TransactionStatus;
import com.example.securetx.transaction.repository.TransactionRepository;

@Service
public class TransactionIdempotencyService {

    private final TransactionRepository transactionRepository;

    public TransactionIdempotencyService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TxCreateResult getOrCreateDepositTx(
            String idempotencyKey,
            DepositRequest req,
            BigDecimal amount,
            String requestHash
    ) {
        Transaction existing = transactionRepository.findByExternalId(idempotencyKey).orElse(null);
        if (existing != null) {
            boolean conflict = existing.getRequestHash() != null && !existing.getRequestHash().equals(requestHash);
            return TxCreateResult.existing(existing.getId(), conflict, existing.getStatus());
        }

        Transaction tx = new Transaction();
        tx.setExternalId(idempotencyKey);
        tx.setFromAccountId(req.getAccountId());
        tx.setToAccountId(req.getAccountId());
        tx.setAmount(amount);
        tx.setCurrency(req.getCurrency());
        tx.setStatus(TransactionStatus.PENDING);
        tx.setCreatedAt(Instant.now());
        tx.setRequestHash(requestHash);

        try {
            Transaction saved = transactionRepository.save(tx);
            return TxCreateResult.created(saved.getId());
        } catch (DataIntegrityViolationException e) {
            Transaction nowExisting = transactionRepository.findByExternalId(idempotencyKey).orElseThrow(() -> e);
            boolean conflict = nowExisting.getRequestHash() != null && !nowExisting.getRequestHash().equals(requestHash);
            return TxCreateResult.existing(nowExisting.getId(), conflict, nowExisting.getStatus());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TxCreateResult getOrCreateTransferTx(
            String idempotencyKey,
            TransferRequest req,
            BigDecimal amount,
            String requestHash
    ) {
        Transaction existing = transactionRepository.findByExternalId(idempotencyKey).orElse(null);
        if (existing != null) {
            boolean conflict = existing.getRequestHash() != null && !existing.getRequestHash().equals(requestHash);
            return TxCreateResult.existing(existing.getId(), conflict, existing.getStatus());
        }

        Transaction tx = new Transaction();
        tx.setExternalId(idempotencyKey);
        tx.setFromAccountId(req.getFromAccountId());
        tx.setToAccountId(req.getToAccountId());
        tx.setAmount(amount);
        tx.setCurrency(req.getCurrency());
        tx.setStatus(TransactionStatus.PENDING);
        tx.setCreatedAt(Instant.now());
        tx.setRequestHash(requestHash);

        try {
            Transaction saved = transactionRepository.save(tx);
            return TxCreateResult.created(saved.getId());
        } catch (DataIntegrityViolationException e) {
            Transaction nowExisting = transactionRepository.findByExternalId(idempotencyKey).orElseThrow(() -> e);
            boolean conflict = nowExisting.getRequestHash() != null && !nowExisting.getRequestHash().equals(requestHash);
            return TxCreateResult.existing(nowExisting.getId(), conflict, nowExisting.getStatus());
        }
    }
    
    
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(Long txId, String reason) {
		Transaction tx = transactionRepository.findById(txId).orElseThrow();
		tx.setStatus(TransactionStatus.FAILED);
		tx.setFailureReason(reason);
		transactionRepository.save(tx);
	}
}
