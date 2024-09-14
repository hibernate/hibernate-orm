/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain;

import java.util.Objects;
import jakarta.persistence.metamodel.BasicType;

import org.hibernate.HibernateException;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.OutputableType;
import org.hibernate.query.sqm.SqmExpressible;

/**
 * Hibernate extension to the JPA {@link BasicType} contract.
 *
 * @author Steve Ebersole
 */
public interface BasicDomainType<J>
		extends SimpleDomainType<J>, BasicType<J>, SqmExpressible<J>, OutputableType<J>, ReturnableType<J> {
	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	default boolean areEqual(J x, J y) throws HibernateException {
		return Objects.equals( x, y );
	}
}
