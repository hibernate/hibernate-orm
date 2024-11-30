/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * Extension of the JPA-defined {@link SingularAttribute} interface.
 *
 * @author Steve Ebersole
 */
public interface SingularPersistentAttribute<D,J>
		extends SingularAttribute<D,J>, PersistentAttribute<D,J>, SqmPathSource<J>, SqmJoinable<D,J> {
	@Override
	SimpleDomainType<J> getType();

	@Override
	ManagedDomainType<D> getDeclaringType();

	@Override
	DomainType<J> getSqmPathType();

	SqmPathSource<J> getPathSource();

	/**
	 * For a singular attribute, the value type is defined as the
	 * attribute type
	 */
	@Override
	default DomainType<?> getValueGraphType() {
		return getType();
	}

	@Override
	default Class<J> getJavaType() {
		return getType().getJavaType();
	}
}
