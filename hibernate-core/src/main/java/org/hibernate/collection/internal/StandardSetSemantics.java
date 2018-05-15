/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardSetSemantics extends AbstractSetSemantics<Set<?>> {
	/**
	 * Singleton access
	 */
	public static final StandardSetSemantics INSTANCE = new StandardSetSemantics();

	private StandardSetSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SET;
	}

	@Override
	public Set<?> instantiateRaw(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		return anticipatedSize < 1 ? new HashSet<>() : new HashSet<>( anticipatedSize );
	}

	@Override
	public <E> PersistentCollection<E> instantiateWrapper(
			Object key,
			PersistentCollectionDescriptor<?, Set<?>, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSet<>( session, collectionDescriptor, key );
	}

	@Override
	public <E> PersistentCollection<E> wrap(
			Set<?> rawCollection,
			PersistentCollectionDescriptor<?, Set<?>, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSet<>( session, collectionDescriptor, rawCollection );
	}

}
