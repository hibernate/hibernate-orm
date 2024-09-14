/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
