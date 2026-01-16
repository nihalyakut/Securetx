package com.example.securetx.audit.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.securetx.audit.entity.AuditLog;
import com.example.securetx.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditService {

	private static final Logger log = LoggerFactory.getLogger(AuditService.class);

	private final AuditLogRepository auditLogRepository;
	private final ObjectMapper objectMapper;

	public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
		this.auditLogRepository = auditLogRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void log(Long userId, String action, Object metadata) {
		try {
			String json = (metadata == null) ? null : objectMapper.writeValueAsString(metadata);

			AuditLog a = new AuditLog();
			a.setUserId(userId);
			a.setAction(action);
			a.setRequestMetadata(json);
			a.setCreatedAt(Instant.now());

			auditLogRepository.save(a);

		} catch (Exception e) {
			log.error("AUDIT_WRITE_FAILED userId={} action={} metadata={}", userId, action, metadata, e);
		}

	}
}
