/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardSortedMapSemantics extends AbstractMapSemantics<SortedMap<?,?>> {
	/**
	 * Singleton access
	 */
	public static final StandardSortedMapSemantics INSTANCE = new StandardSortedMapSemantics();

	private StandardSortedMapSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SORTED_MAP;
	}

	@Override
	public TreeMap<?, ?> instantiateRaw(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		return new TreeMap<>();
	}

	@Override
	public PersistentCollection instantiateWrapper(
			Object key,
			PersistentCollectionDescriptor collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSortedMap( session, collectionDescriptor, key );
	}

	@Override
	public PersistentCollection wrap(
			SortedMap rawCollection,
			PersistentCollectionDescriptor collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSortedMap( session, collectionDescriptor, rawCollection );
	}
}
