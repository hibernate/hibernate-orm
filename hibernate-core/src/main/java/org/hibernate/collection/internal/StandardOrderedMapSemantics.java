/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardOrderedMapSemantics extends AbstractMapSemantics<LinkedHashMap<?,?>> {
	/**
	 * Singleton access
	 */
	public static final StandardOrderedMapSemantics INSTANCE = new StandardOrderedMapSemantics();

	private StandardOrderedMapSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ORDERED_MAP;
	}

	@Override
	public LinkedHashMap<?, ?> instantiateRaw(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		return anticipatedSize < 1 ? new LinkedHashMap<>() : new LinkedHashMap<>( anticipatedSize );
	}

	@Override
	public <E> PersistentCollection<E> instantiateWrapper(
			Object key,
			PersistentCollectionDescriptor<?, LinkedHashMap<?, ?>, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentMap<>( session, collectionDescriptor, key );
	}

	@Override
	public <E> PersistentCollection<E> wrap(
			LinkedHashMap<?, ?> rawCollection,
			PersistentCollectionDescriptor<?, LinkedHashMap<?, ?>, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentMap<>( session, collectionDescriptor, rawCollection );
	}

	@Override
	public Iterator getElementIterator(LinkedHashMap rawCollection) {
		return rawCollection.entrySet().iterator();
	}
}
