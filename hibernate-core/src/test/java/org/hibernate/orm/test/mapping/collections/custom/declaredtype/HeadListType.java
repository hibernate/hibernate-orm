/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

/**
 * @author Steve Ebersole
 */
public class HeadListType implements UserCollectionType {

	@Override
	public CollectionClassification getClassification() {
		return CollectionClassification.LIST;
	}

	@Override
	public Class<?> getCollectionClass() {
		return IHeadList.class;
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister) throws HibernateException {
		return new PersistentHeadList( session );
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentHeadList( session, (IHeadList) collection );
	}

	public Iterator getElementsIterator(Object collection) {
		return ( (IHeadList) collection ).iterator();
	}

	public boolean contains(Object collection, Object entity) {
		return ( (IHeadList) collection ).contains( entity );
	}

	public Object indexOf(Object collection, Object entity) {
		int l = ( (IHeadList) collection ).indexOf( entity );
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
		IHeadList result = (IHeadList) target;
		result.clear();
		result.addAll( (HeadList) original );
		return result;
	}

	public Object instantiate(int anticipatedSize) {
		return new HeadList();
	}


}
