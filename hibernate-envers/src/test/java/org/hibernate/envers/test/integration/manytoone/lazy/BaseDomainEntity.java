/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.manytoone.lazy;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Chris Cranford
 */
@MappedSuperclass
public abstract class BaseDomainEntity extends BaseDomainEntityMetadata {
	private static final long serialVersionUID = 1023010094948580516L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected long id = 0;

	BaseDomainEntity() {

	}

	BaseDomainEntity(Instant timestamp, String who) {
		super( timestamp, who );
	}

	public long getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		BaseDomainEntity that = (BaseDomainEntity) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}
}
