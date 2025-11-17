/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.Collection;

import org.hibernate.collection.spi.AbstractBagSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentIdentifierBag;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * CollectionSemantics implementation for id-bags
 *
 * @author Steve Ebersole
 */
public class StandardIdentifierBagSemantics<E> extends AbstractBagSemantics<E> {
	/**
	 * Singleton access
	 */
	public static final StandardIdentifierBagSemantics<?> INSTANCE = new StandardIdentifierBagSemantics<>();

	private StandardIdentifierBagSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ID_BAG;
	}

	@Override
	public PersistentCollection<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentIdentifierBag<>( session );
	}

	@Override
	public PersistentCollection<E> wrap(
			Collection<E> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentIdentifierBag<>( session, rawCollection );
	}
}
