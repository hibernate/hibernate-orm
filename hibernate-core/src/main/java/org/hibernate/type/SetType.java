/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

public class SetType extends CollectionType {

	public SetType(String role, String propertyRef) {
		super(role, propertyRef );
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SET;
	}

	@Override
	public Class<?> getReturnedClass() {
		return java.util.Set.class;
	}

	@Override
	public PersistentCollection<?> instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Object key) {
		return new PersistentSet<>( session );
	}

	@Override
	public PersistentCollection<?> wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentSet<>( session, (java.util.Set<?>) collection );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0
				? CollectionHelper.set()
				: CollectionHelper.setOfSize( anticipatedSize );
	}

}
