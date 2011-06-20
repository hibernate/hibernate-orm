/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

	public MapType(TypeFactory.TypeScope typeScope, String role, String propertyRef, boolean isEmbeddedInXML) {
		super( typeScope, role, propertyRef, isEmbeddedInXML );
	}

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) {
		return new PersistentMap(session);
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
