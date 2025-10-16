/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.lazy;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * @author Chris Cranford
 */
@Entity
@Table(name = "shipment", uniqueConstraints = @UniqueConstraint(columnNames = { "identifier" }))
@Audited
@AuditTable(value = "shipment_audit")
public class Shipment extends BaseDomainEntity {
	private static final long serialVersionUID = 5061763935663020703L;

	@Column(name = "due_date", nullable = false, updatable = false)
	private Instant dueDate;

	@Column(name = "identifier", nullable = false, updatable = false)
	private String identifier;

	@Version
	@Column(name = "mvc_version", nullable = false)
	private Long mvcVersion;

	@Column(name = "closed")
	private Boolean closed;

	@ManyToOne(optional = true, fetch = FetchType.LAZY, targetEntity = AddressVersion.class)
	@JoinColumns(value = {
			@JoinColumn(name = "origin_address_id", referencedColumnName = "id", nullable = true),
			@JoinColumn(name = "origin_address_version", referencedColumnName = "version", nullable = true)
	})
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private AddressVersion origin;

	@ManyToOne(optional = true, fetch = FetchType.LAZY, targetEntity = AddressVersion.class)
	@JoinColumns(value = {
			@JoinColumn(name = "destination_address_id", referencedColumnName = "id", nullable = true),
			@JoinColumn(name = "destination_address_version", referencedColumnName = "version", nullable = true)
	})
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private AddressVersion destination;

	@ManyToOne(optional = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private User user;

	Shipment() {
	}

	Shipment(Instant when, String who, Instant dueDate, String identifier, AddressVersion origin, AddressVersion dest) {
		super( when, who );
		this.dueDate = dueDate;
		this.identifier = Objects.requireNonNull( identifier );
		this.origin = origin;
		this.destination = dest;
	}

	public Instant getDueDate() {
		return dueDate;
	}

	public String getIdentifier() {
		return identifier;
	}

	public Boolean getClosed() {
		return closed;
	}

	public void setClosed(Boolean closed) {
		this.closed = closed;
	}

	public AddressVersion getOrigin() {
		return origin;
	}

	public void setOrigin(AddressVersion origin) {
		this.origin = origin;
	}

	public AddressVersion getDestination() {
		return destination;
	}

	public void setDestination(AddressVersion destination) {
		this.destination = destination;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
