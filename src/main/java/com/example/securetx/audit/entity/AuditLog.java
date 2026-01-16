package com.example.securetx.audit.entity;

import jakarta.persistence.*;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id")
	private Long userId;

	@Column(nullable = false, length = 100)
	private String action;

    @JdbcTypeCode(SqlTypes.JSON)               
    @Column(name = "request_metadata", columnDefinition = "jsonb")
    private String requestMetadata;             
    
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getRequestMetadata() {
		return requestMetadata;
	}

	public void setRequestMetadata(String requestMetadata) {
		this.requestMetadata = requestMetadata;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
