/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.Collection;
import jakarta.persistence.metamodel.CollectionAttribute;

/**
 * Hibernate extension to the JPA {@link CollectionAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface BagPersistentAttribute<D,E>
		extends CollectionAttribute<D,E>, PluralPersistentAttribute<D,Collection<E>,E> {
	@Override
	SimpleDomainType<E> getValueGraphType();

	@Override
	SimpleDomainType<E> getElementType();

	@Override
	ManagedDomainType<D> getDeclaringType();
}
