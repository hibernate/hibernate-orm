/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.Objects;
import jakarta.persistence.metamodel.BasicType;

import org.hibernate.HibernateException;
import org.hibernate.query.OutputableType;

/**
 * Hibernate extension to the JPA {@link BasicType} contract.
 *
 * @author Steve Ebersole
 */
public interface BasicDomainType<J>
		extends ReturnableType<J>, BasicType<J>, OutputableType<J> {
	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	default boolean areEqual(J x, J y) throws HibernateException {
		return Objects.equals( x, y );
	}
}
