package org.hibernate.test.collection.custom.declaredtype;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

import java.util.Iterator;
import java.util.Map;

public class HeadListType implements UserCollectionType {

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister) throws HibernateException {
		return new PersistentHeadList(session);
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new PersistentHeadList( session, (IHeadList) collection );
	}

	public Iterator getElementsIterator(Object collection) {
		return ( (IHeadList) collection ).iterator();
	}

	public boolean contains(Object collection, Object entity) {
		return ( (IHeadList) collection ).contains(entity);
	}

	public Object indexOf(Object collection, Object entity) {
		int l = ( (IHeadList) collection ).indexOf(entity);
		if(l<0) {
			return null;
		} else {
			return l;
		}
	}

	public Object replaceElements(Object original, Object target, CollectionPersister persister, Object owner, Map copyCache, SessionImplementor session) throws HibernateException {
		IHeadList result = (IHeadList) target;
		result.clear();
		result.addAll((HeadList)original);
		return result;
	}

	public Object instantiate(int anticipatedSize) {
		return new HeadList();
	}

	
}
