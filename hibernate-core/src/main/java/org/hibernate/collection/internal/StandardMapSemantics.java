/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Map;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * CollectionSemantics for maps
 *
 * @author Steve Ebersole
 */
public class StandardMapSemantics extends AbstractMapSemantics<Map<?,?>> {
	/**
	 * Singleton access
	 */
	public static final StandardMapSemantics INSTANCE = new StandardMapSemantics();

	private StandardMapSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.MAP;
	}

	@Override
	public Map<?, ?> instantiateRaw(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		return CollectionHelper.mapOfSize( anticipatedSize );
	}

	@Override
	public <E> PersistentCollection<E> instantiateWrapper(
			Object key,
			PersistentCollectionDescriptor<?, Map<?, ?>, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentMap<>( session, collectionDescriptor, key );
	}

	@Override
	public <E> PersistentCollection<E> wrap(
			Map<?, ?> rawCollection,
			PersistentCollectionDescriptor<?, Map<?, ?>, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentMap<>( session, collectionDescriptor, rawCollection );
	}
}
