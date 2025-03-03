/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.util.ArrayList;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentIdentifierBag;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

public class IdentifierBagType extends CollectionType {

	public IdentifierBagType(String role, String propertyRef) {
		super(role, propertyRef );
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ID_BAG;
	}

	@Override
	public PersistentCollection<?> instantiate(
			SharedSessionContractImplementor session,
			CollectionPersister persister, Object key)
		throws HibernateException {

		return new PersistentIdentifierBag<>( session );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ArrayList<>() : new ArrayList<>( anticipatedSize + 1 );
	}

	@Override
	public Class<?> getReturnedClass() {
		return java.util.Collection.class;
	}

	@Override
	public PersistentCollection<?> wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentIdentifierBag<>( session, (java.util.Collection<?>) collection );
	}

}
