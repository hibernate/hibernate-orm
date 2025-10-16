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
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Chris Cranford
 */
@Entity
@Table(name = "address_version")
public class AddressVersion extends BaseDomainEntityVersion {
	private static final long serialVersionUID = 1100389518057335117L;

	@Id
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "id", referencedColumnName = "id", updatable = false, nullable = false)
	private Address id;

	@Column(name = "description", updatable = false)
	private String description;

	AddressVersion() {
	}

	AddressVersion(Instant when, String who, Address id, long version, String description) {
		setCreatedAt( when );
		setCreatedBy( who );
		setVersion( version );
		this.id = Objects.requireNonNull(id );
		this.description = description;
	}

	@Override
	public Address getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public AddressVersion update(Instant when, String who, String description) {
		AddressVersion version = new AddressVersion( when, who, id, getVersion() + 1, description );
		id.versions.add( version );
		return version;
	}
}
