/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.Collection;

import jakarta.annotation.Nonnull;
import jakarta.persistence.metamodel.CollectionAttribute;

/**
 * Hibernate extension to the JPA {@link CollectionAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface BagPersistentAttribute<D,E>
		extends CollectionAttribute<D,E>, PluralPersistentAttribute<D,Collection<E>,E> {
	@Override
	@Nonnull
	SimpleDomainType<E> getValueGraphType();

	@Override
	@Nonnull
	SimpleDomainType<E> getElementType();

	@Override
	@Nonnull
	ManagedDomainType<D> getDeclaringType();
}
