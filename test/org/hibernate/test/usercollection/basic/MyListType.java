package org.hibernate.test.usercollection.basic;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

public class MyListType implements UserCollectionType {

	static int lastInstantiationRequest = -2;

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister) throws HibernateException {
		return new PersistentMyList(session);
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			throw new IllegalStateException("dom4j not supported");
		}
		else {
			return new PersistentMyList( session, (IMyList) collection );
		}
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
