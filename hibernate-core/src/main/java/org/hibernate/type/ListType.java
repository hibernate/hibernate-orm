/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.collection.spi.PersistentList;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

import static org.hibernate.metamodel.CollectionClassification.LIST;

public class ListType extends CollectionType {

	public ListType(String role, String propertyRef) {
		super(role, propertyRef );
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return LIST;
	}

	@Override
	public PersistentCollection<?> instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Object key) {
		return new PersistentList<>( session );
	}

	@Override
	public Class<?> getReturnedClass() {
		return List.class;
	}

	@Override
	public PersistentCollection<?> wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentList<>( session, (List<?>) collection );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ArrayList<>() : new ArrayList<>( anticipatedSize + 1 );
	}

	@Override
	public Object indexOf(Object collection, Object element) {
		final var list = (List<?>) collection;
		for ( int i=0; i<list.size(); i++ ) {
			//TODO: proxies!
			if ( list.get(i) == element ) {
				return i;
			}
		}
		return null;
	}
}
