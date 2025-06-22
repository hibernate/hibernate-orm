/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.mappedsuperclass;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@MappedSuperclass
@Access(AccessType.FIELD)
@Audited
public abstract class AbstractAuditedEmbeddable {

	/**
	 * Initial Value
	 */
	protected static final int UNDEFINED = -1;

	private int code = UNDEFINED;


	protected AbstractAuditedEmbeddable() {
		this( UNDEFINED );
	}

	/**
	 * Constructor with code
	 */
	public AbstractAuditedEmbeddable(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + code;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		AbstractAuditedEmbeddable other = (AbstractAuditedEmbeddable) obj;
		if ( code != other.code ) {
			return false;
		}
		return true;
	}
}
