//$Id: MapType.java 10086 2006-07-05 18:17:27Z steve.ebersole@jboss.com $
package org.hibernate.type;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Element;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.collection.PersistentMap;
import org.hibernate.collection.PersistentMapElementHolder;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;


public class MapType extends CollectionType {

	public MapType(String role, String propertyRef, boolean isEmbeddedInXML) {
		super(role, propertyRef, isEmbeddedInXML);
	}

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			return new PersistentMapElementHolder(session, persister, key);
		}
		else {
			return new PersistentMap(session);
		}
	}

	public Class getReturnedClass() {
		return Map.class;
	}

	public Iterator getElementsIterator(Object collection) {
		return ( (java.util.Map) collection ).values().iterator();
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			return new PersistentMapElementHolder( session, (Element) collection );
		}
		else {
			return new PersistentMap( session, (java.util.Map) collection );
		}
	}
	
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 
		       ? new HashMap()
		       : new HashMap( anticipatedSize + (int)( anticipatedSize * .75f ), .75f );
	}

	public Object replaceElements(
		final Object original,
		final Object target,
		final Object owner, 
		final java.util.Map copyCache, 
		final SessionImplementor session)
		throws HibernateException {

		CollectionPersister cp = session.getFactory().getCollectionPersister( getRole() );
		
		java.util.Map result = (java.util.Map) target;
		result.clear();
		
		Iterator iter = ( (java.util.Map) original ).entrySet().iterator();
		while ( iter.hasNext() ) {
			java.util.Map.Entry me = (java.util.Map.Entry) iter.next();
			Object key = cp.getIndexType().replace( me.getKey(), null, session, owner, copyCache );
			Object value = cp.getElementType().replace( me.getValue(), null, session, owner, copyCache );
			result.put(key, value);
		}
		
		return result;
		
	}
	
	public Object indexOf(Object collection, Object element) {
		Iterator iter = ( (Map) collection ).entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			//TODO: proxies!
			if ( me.getValue()==element ) return me.getKey();
		}
		return null;
	}

}
