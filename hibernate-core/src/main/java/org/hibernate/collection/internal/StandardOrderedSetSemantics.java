/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import org.hibernate.collection.spi.AbstractSetSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * @author Steve Ebersole
 */
public class StandardOrderedSetSemantics<E> extends AbstractSetSemantics<LinkedHashSet<E>,E> {
	/**
	 * Singleton access
	 */
	public static final StandardOrderedSetSemantics<?> INSTANCE = new StandardOrderedSetSemantics<>();

	private StandardOrderedSetSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ORDERED_SET;
	}

	@Override
	public LinkedHashSet<E> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return anticipatedSize < 1 ? CollectionHelper.linkedSet() : CollectionHelper.linkedSetOfSize( anticipatedSize );
	}

	@Override
	public boolean isMutableRaw(Object collection) {
		return collection.getClass() == LinkedHashSet.class;
	}

	@Override
	public PersistentCollection<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSet<>( session );
	}

	@Override
	public PersistentCollection<E> wrap(
			LinkedHashSet<E> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSet<>( session, collectionDescriptor, rawCollection );
	}

	@Override
	public Iterator<E> getElementIterator(LinkedHashSet<E> rawCollection) {
		return rawCollection.iterator();
	}
}
