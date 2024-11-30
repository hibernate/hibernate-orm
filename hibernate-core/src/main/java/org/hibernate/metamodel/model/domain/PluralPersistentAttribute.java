/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import jakarta.persistence.metamodel.PluralAttribute;

import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.query.NotIndexedCollectionException;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * Extension of the JPA-defined {@link PluralAttribute} interface.
 *
 * @author Steve Ebersole
 */
public interface PluralPersistentAttribute<D, C, E>
		extends PersistentAttribute<D, C>, SqmPathSource<E>, SqmJoinable<D,E>, PluralAttribute<D, C, E> {
	@Override
	ManagedDomainType<D> getDeclaringType();

	CollectionClassification getCollectionClassification();

	SqmPathSource<E> getElementPathSource();

	default SqmPathSource<?> getIndexPathSource() {
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
