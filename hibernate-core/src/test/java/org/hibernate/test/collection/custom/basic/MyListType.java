/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.custom.basic;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

public class MyListType implements UserCollectionType {

	static int lastInstantiationRequest = -2;

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister) throws HibernateException {
		return new PersistentMyList(session);
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new PersistentMyList( session, (IMyList) collection );
	}

	public Iterator getElementsIterator(Object collection) {
		return ( (IMyList) collection ).iterator();
	}

	public boolean contains(Object collection, Object entity) {
		return ( (IMyList) collection ).contains(entity);
	}

	public Object indexOf(Object collection, Object entity) {
		int l = ( (IMyList) collection ).indexOf(entity);
		if(l<0) {
			return null;
		} else {
			return new Integer(l);
		}
	}

	public Object replaceElements(Object original, Object target, CollectionPersister persister, Object owner, Map copyCache, SessionImplementor session) throws HibernateException {
		IMyList result = (IMyList) target;
		result.clear();
		result.addAll((MyList)original);
		return result;
	}

	public Object instantiate(int anticipatedSize) {
		lastInstantiationRequest = anticipatedSize;
		return new MyList();
	}

	
}
