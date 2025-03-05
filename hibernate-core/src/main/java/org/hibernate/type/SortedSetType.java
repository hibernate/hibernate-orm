/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.util.Comparator;
import java.util.TreeSet;

import org.hibernate.collection.spi.PersistentSortedSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

public class SortedSetType extends SetType {

	private final Comparator<?> comparator;

	public SortedSetType( String role, String propertyRef, Comparator<?> comparator) {
		super( role, propertyRef );
		this.comparator = comparator;
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SORTED_SET;
	}

	public Class<?> getReturnedClass() {
		return java.util.SortedSet.class;
	}

	@Override
	public PersistentCollection<?> instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Object key) {
		return new PersistentSortedSet<>( session, comparator );
	}

	public Object instantiate(int anticipatedSize) {
		return new TreeSet<>(comparator);
	}

	@Override
	public PersistentCollection<?> wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentSortedSet<>( session, (java.util.SortedSet<?>) collection );
	}
}
