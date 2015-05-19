/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;


public class MapType extends CollectionType {

	/**
	 * @deprecated Use {@link #MapType(TypeFactory.TypeScope, String, String) } instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public MapType(TypeFactory.TypeScope typeScope, String role, String propertyRef, boolean isEmbeddedInXML) {
		super( typeScope, role, propertyRef, isEmbeddedInXML );
	}

	public MapType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		super( typeScope, role, propertyRef );
	}

	public PersistentCollection instantiate(
			SessionImplementor session,
			CollectionPersister persister,
			Serializable key) {
		return new PersistentMap( session );
	}

	public Class getReturnedClass() {
		return Map.class;
	}

	public Iterator getElementsIterator(Object collection) {
		return ( (java.util.Map) collection ).values().iterator();
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new PersistentMap( session, (java.util.Map) collection );
	}

	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0
				? new HashMap()
				: new HashMap( anticipatedSize + (int) ( anticipatedSize * .75f ), .75f );
	}

	public Object replaceElements(
			final Object original,
			final Object target,
			final Object owner,
			final java.util.Map copyCache,
			final SessionImplementor session) throws HibernateException {
		CollectionPersister cp = session.getFactory().getCollectionPersister( getRole() );

		java.util.Map result = (java.util.Map) target;
		result.clear();

		for ( Object o : ( (Map) original ).entrySet() ) {
			Map.Entry me = (Map.Entry) o;
			Object key = cp.getIndexType().replace( me.getKey(), null, session, owner, copyCache );
			Object value = cp.getElementType().replace( me.getValue(), null, session, owner, copyCache );
			result.put( key, value );
		}

		return result;

	}

	public Object indexOf(Object collection, Object element) {
		Iterator iter = ( (Map) collection ).entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			//TODO: proxies!
			if ( me.getValue() == element ) {
				return me.getKey();
			}
		}
		return null;
	}

}
