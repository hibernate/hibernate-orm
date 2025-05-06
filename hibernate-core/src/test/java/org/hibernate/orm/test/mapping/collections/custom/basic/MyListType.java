/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.basic;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

public class MyListType implements UserCollectionType {

	static int lastInstantiationRequest = -2;

	@Override
	public CollectionClassification getClassification() {
		return CollectionClassification.LIST;
	}

	@Override
	public Class<?> getCollectionClass() {
		return IMyList.class;
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister) throws HibernateException {
		return new PersistentMyList( session );
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentMyList( session, (IMyList) collection );
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return ( (IMyList) collection ).iterator();
	}

	@Override
	public boolean contains(Object collection, Object entity) {
		return ( (IMyList) collection ).contains(entity);
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		int l = ( (IMyList) collection ).indexOf(entity);
		if(l<0) {
			return null;
		} else {
			return new Integer(l);
		}
	}

	@Override
	public Object replaceElements(Object original, Object target, CollectionPersister persister, Object owner, Map copyCache, SharedSessionContractImplementor session) throws HibernateException {
		IMyList result = (IMyList) target;
		result.clear();
		result.addAll((MyList)original);
		return result;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		lastInstantiationRequest = anticipatedSize;
		return new MyList();
	}

}
