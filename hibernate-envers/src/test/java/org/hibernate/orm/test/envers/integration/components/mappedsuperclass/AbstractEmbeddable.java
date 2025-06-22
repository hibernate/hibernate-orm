/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.mappedsuperclass;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Jakob Braeuchi.
 */

@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class AbstractEmbeddable {

	/**
	 * Initial Value
	 */
	protected static final int UNDEFINED = -1;

	private int code = UNDEFINED;


	protected AbstractEmbeddable() {
		this( UNDEFINED );
	}

	/**
	 * Constructor with code
	 */
	public AbstractEmbeddable(int code) {
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
		AbstractEmbeddable other = (AbstractEmbeddable) obj;
		if ( code != other.code ) {
			return false;
		}
		return true;
	}
}
