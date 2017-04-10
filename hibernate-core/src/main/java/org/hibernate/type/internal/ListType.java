/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.spi.CollectionPersister;

/**
 * @author Andrea Boriero
 */
public class ListType extends AbstractCollectionType {
	public ListType(String roleName) {
		super( roleName );
	}

	@Override
	public Object indexOf(Object collection, Object element) {List list = (List) collection;
		for ( int i=0; i<list.size(); i++ ) {
			//TODO: proxies!
			if ( list.get(i)==element ) {
				return i;
			}
		}
		return null;
	}

	@Override
	public PersistentCollection instantiate(
			SharedSessionContractImplementor session, CollectionPersister persister, Serializable key) {
		return new PersistentList( session );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ArrayList() : new ArrayList( anticipatedSize + 1 );
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentList( session, (List) collection );
	}

	@Override
	public Class getReturnedClass() {
		return List.class;
	}
}
