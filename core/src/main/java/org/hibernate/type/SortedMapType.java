/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Comparator;
import java.util.TreeMap;

import org.dom4j.Element;
import org.hibernate.EntityMode;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.collection.PersistentElementHolder;
import org.hibernate.collection.PersistentMapElementHolder;
import org.hibernate.collection.PersistentSortedMap;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;


public class SortedMapType extends MapType {

	private final Comparator comparator;

	public SortedMapType(String role, String propertyRef, Comparator comparator, boolean isEmbeddedInXML) {
		super(role, propertyRef, isEmbeddedInXML);
		this.comparator = comparator;
	}

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			return new PersistentMapElementHolder(session, persister, key);
		}
		else {
			PersistentSortedMap map = new PersistentSortedMap(session);
			map.setComparator(comparator);
			return map;
		}
	}

	public Class getReturnedClass() {
		return java.util.SortedMap.class;
	}

	public Object instantiate(int anticipatedSize) {
		return new TreeMap(comparator);
	}
	
	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			return new PersistentElementHolder( session, (Element) collection );
		}
		else {
			return new PersistentSortedMap( session, (java.util.SortedMap) collection );
		}
	}

}






