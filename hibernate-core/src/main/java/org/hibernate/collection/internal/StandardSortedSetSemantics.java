/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.collection.spi.AbstractSetSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentSortedSet;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public class StandardSortedSetSemantics<E> extends AbstractSetSemantics<SortedSet<E>,E> {
	/**
	 * Singleton access
	 */
	public static final StandardSortedSetSemantics<?> INSTANCE = new StandardSortedSetSemantics<>();

	private StandardSortedSetSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SORTED_SET;
	}

	@Override
	public Class<SortedSet> getCollectionJavaType() {
		return SortedSet.class;
	}

	@Override
	public SortedSet<E> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return new TreeSet<E>(
				collectionDescriptor == null ? null : (Comparator) collectionDescriptor.getSortingComparator()
		);
	}

	@Override
	public PersistentCollection<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		//noinspection unchecked
		return new PersistentSortedSet<>( session, (Comparator<E>) collectionDescriptor.getSortingComparator() );
	}

	@Override
	public PersistentCollection<E> wrap(
			SortedSet<E> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSortedSet<>( session, rawCollection );
	}

	@Override
	public Iterator<E> getElementIterator(SortedSet<E> rawCollection) {
		return rawCollection.iterator();
	}
}
