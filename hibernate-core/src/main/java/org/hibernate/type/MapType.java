/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;


public class MapType extends CollectionType {

	public MapType(String role, String propertyRef) {
		super(role, propertyRef );
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.MAP;
	}

	@Override
	public PersistentCollection<?> instantiate(
			SharedSessionContractImplementor session,
			CollectionPersister persister,
			Object key) {
		return new PersistentMap<>( session );
	}

	@Override
	public Class<?> getReturnedClass() {
		return Map.class;
	}

	@Override
	public Iterator<?> getElementsIterator(Object collection) {
		return ( (Map<?,?>) collection ).values().iterator();
	}

	@Override
	public PersistentCollection<?> wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentMap<>( session, (Map<?,?>) collection );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0
				? new HashMap<>()
				: new HashMap<>( anticipatedSize + (int) ( anticipatedSize * .75f ), .75f );
	}

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public Object replaceElements(
			final Object original,
			final Object target,
			final Object owner,
			final Map<Object, Object> copyCache,
			final SharedSessionContractImplementor session) throws HibernateException {
		final CollectionPersister persister =
				session.getFactory().getMappingMetamodel()
						.getCollectionDescriptor( getRole() );

		final Map source = (Map) original;
		final Map result = (Map) target;
		result.clear();

		for ( Object entry : source.entrySet() ) {
			final Map.Entry me = (Map.Entry) entry;
			final Object key = persister.getIndexType().replace( me.getKey(), null, session, owner, copyCache );
			final Object value = persister.getElementType().replace( me.getValue(), null, session, owner, copyCache );
			result.put( key, value );
		}

		return result;

	}

	@Override @SuppressWarnings("rawtypes")
	public Object indexOf(Object collection, Object element) {
		final Map map = (Map) collection;
		for ( Object entry : map.entrySet() ) {
			final Map.Entry me = (Map.Entry) entry;
			//TODO: proxies!
			if ( me.getValue() == element ) {
				return me.getKey();
			}
		}
		return null;
	}

}
