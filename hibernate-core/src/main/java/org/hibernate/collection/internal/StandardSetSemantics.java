/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.collection.spi.AbstractSetSemantics;
import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public class StandardSetSemantics<E> extends AbstractSetSemantics<Set<E>,E> {
	/**
	 * Singleton access
	 */
	public static final StandardSetSemantics<?> INSTANCE = new StandardSetSemantics<>();

	private StandardSetSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SET;
	}

	@Override
	public Set<E> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return anticipatedSize < 1 ? new HashSet<>() : CollectionHelper.setOfSize( anticipatedSize );
	}

	@Override
	public PersistentSet<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSet<>( session );
	}

	@Override
	public PersistentSet<E> wrap(
			Set<E> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSet<>( session, rawCollection );
	}

}
