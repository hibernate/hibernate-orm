/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.manytoone.lazy;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.CreationTimestamp;

/**
 * @author Chris Cranford
 */
@MappedSuperclass
public abstract class BaseDomainEntityMetadata extends Base implements Serializable {
	private static final long serialVersionUID = 2765056578095518489L;

	@Column(name = "created_by", nullable = false, updatable = false)
	private String createdBy;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	BaseDomainEntityMetadata() {

	}

	BaseDomainEntityMetadata(Instant timestamp, String who) {
		this.createdBy = who;
		this.createdAt = timestamp;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
