package org.hibernate.test.collection.custom.declaredtype.explicitsemantics;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

import java.util.Iterator;
import java.util.Map;

public class HeadSetListType implements UserCollectionType {

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister) throws HibernateException {
		return new PersistentHeadList(session);
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new PersistentHeadList( session, (IHeadSetList) collection );
	}

	public Iterator getElementsIterator(Object collection) {
		return ( (IHeadSetList) collection ).iterator();
	}

	public boolean contains(Object collection, Object entity) {
		return ( (IHeadSetList) collection ).contains(entity);
	}

	public Object indexOf(Object collection, Object entity) {
		int l = ( (IHeadSetList) collection ).indexOf(entity);
		if(l<0) {
			return null;
		} else {
			return l;
		}
	}

	public Object replaceElements(Object original, Object target, CollectionPersister persister, Object owner, Map copyCache, SessionImplementor session) throws HibernateException {
		IHeadSetList result = (IHeadSetList) target;
		result.clear();
		result.addAll((HeadSetList)original);
		return result;
	}

	public Object instantiate(int anticipatedSize) {
		return new HeadSetList();
	}

	
}
