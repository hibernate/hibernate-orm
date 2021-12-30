/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.collection.custom.declaredtype.explicitsemantics;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

/**
 * @author Steve Ebersole
 */
public class HeadSetListType implements UserCollectionType {

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister)
			throws HibernateException {
		return new PersistentHeadList( session );
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentHeadList( session, (IHeadSetList) collection );
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return ( (IHeadSetList) collection ).iterator();
	}

	@Override
	public boolean contains(Object collection, Object entity) {
		return ( (IHeadSetList) collection ).contains( entity );
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		int l = ( (IHeadSetList) collection ).indexOf( entity );
		if ( l < 0 ) {
			return null;
		}
		else {
			return l;
		}
	}

	@Override
	public Object replaceElements(
			Object original,
			Object target,
			CollectionPersister persister,
			Object owner,
			Map copyCache,
			SharedSessionContractImplementor session) throws HibernateException {
		IHeadSetList result = (IHeadSetList) target;
		result.clear();
		result.addAll( (HeadSetList) original );
		return result;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return new HeadSetList();
	}
}
