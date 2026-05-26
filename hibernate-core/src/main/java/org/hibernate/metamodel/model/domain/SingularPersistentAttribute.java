/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import jakarta.annotation.Nonnull;
import jakarta.persistence.metamodel.SingularAttribute;


/**
 * Extension of the JPA-defined {@link SingularAttribute} interface.
 *
 * @author Steve Ebersole
 */
public interface SingularPersistentAttribute<D,J>
		extends SingularAttribute<D,J>, PersistentAttribute<D,J>, PathSource<J> {
	@Override
	@Nonnull
	SimpleDomainType<J> getType();

	@Override
	@Nonnull
	ManagedDomainType<D> getDeclaringType();

	/**
	 * For a singular attribute, the value type is defined as the
	 * attribute type
	 */
	@Override
	@Nonnull
	default DomainType<J> getValueGraphType() {
		return getType();
	}

	@Override
	@Nonnull
	default Class<J> getJavaType() {
		return getType().getJavaType();
	}
}
