/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.hibernate.collection.spi.AbstractMapSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public class StandardOrderedMapSemantics<K,V> extends AbstractMapSemantics<LinkedHashMap<K,V>,K,V> {
	/**
	 * Singleton access
	 */
	public static final StandardOrderedMapSemantics<?,?> INSTANCE = new StandardOrderedMapSemantics<>();

	private StandardOrderedMapSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ORDERED_MAP;
	}

	@Override
	public LinkedHashMap<K,V> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return anticipatedSize < 1 ? CollectionHelper.linkedMap() : CollectionHelper.linkedMapOfSize( anticipatedSize );
	}

	@Override
	public PersistentCollection<V> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentMap<>( session );
	}

	@Override
	public PersistentCollection<V> wrap(
			LinkedHashMap<K,V> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentMap<>( session, rawCollection );
	}

	@Override
	public Iterator<V> getElementIterator(LinkedHashMap<K,V> rawCollection) {
		return rawCollection.values().iterator();
	}
}
