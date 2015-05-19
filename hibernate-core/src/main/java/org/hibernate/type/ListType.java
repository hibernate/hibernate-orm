/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

public class ListType extends CollectionType {

	/**
	 * @deprecated Use {@link #ListType(org.hibernate.type.TypeFactory.TypeScope, String, String)}
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public ListType(TypeFactory.TypeScope typeScope, String role, String propertyRef, boolean isEmbeddedInXML) {
		super( typeScope, role, propertyRef, isEmbeddedInXML );
	}

	public ListType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		super( typeScope, role, propertyRef );
	}

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) {
		return new PersistentList(session);
	}

	public Class getReturnedClass() {
		return List.class;
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new PersistentList( session, (List) collection );
	}

	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ArrayList() : new ArrayList( anticipatedSize + 1 );
	}
	
	public Object indexOf(Object collection, Object element) {
		List list = (List) collection;
		for ( int i=0; i<list.size(); i++ ) {
			//TODO: proxies!
			if ( list.get(i)==element ) {
				return i;
			}
		}
		return null;
	}
	
}

