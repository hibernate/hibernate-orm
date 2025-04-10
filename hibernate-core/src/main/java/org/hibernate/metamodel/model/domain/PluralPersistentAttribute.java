/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import jakarta.persistence.metamodel.PluralAttribute;

import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.query.NotIndexedCollectionException;

/**
 * Extension of the JPA-defined {@link PluralAttribute} interface.
 *
 * @author Steve Ebersole
 */
public interface PluralPersistentAttribute<D, C, E>
		extends PersistentAttribute<D, C>, PathSource<E>, PluralAttribute<D, C, E> {
	@Override
	ManagedDomainType<D> getDeclaringType();

	CollectionClassification getCollectionClassification();

	PathSource<E> getElementPathSource();

	default PathSource<?> getIndexPathSource() {
		throw new NotIndexedCollectionException(
				"Plural attribute [" +  getPathName() + "] is not indexed (list / map)"
		);
	}

	@Override
	SimpleDomainType<E> getElementType();

	@Override
	SimpleDomainType<E> getValueGraphType();

	default SimpleDomainType<?> getKeyGraphType() {
		throw new NotIndexedCollectionException(
				"Plural attribute [" +  getPathName() + "] is not indexed (list / map)"
		);
	}
}
