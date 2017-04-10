/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.Comparator;
import java.util.TreeMap;

import org.hibernate.collection.internal.PersistentSortedMap;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.spi.CollectionPersister;

/**
 * @author Andrea Boriero
 */
public class SortedMapType extends MapType {
	public SortedMapType(String roleName, Comparator comparator) {
		super( roleName, comparator );
	}

	@Override
	public PersistentCollection instantiate(
			SharedSessionContractImplementor session,
			CollectionPersister persister,
			Serializable key) {
		PersistentSortedMap map = new PersistentSortedMap( session );
		map.setComparator( getComparator() );
		return map;
	}

	public Class getReturnedClass() {
		return java.util.SortedMap.class;
	}

	@SuppressWarnings({"unchecked"})
	public Object instantiate(int anticipatedSize) {
		return new TreeMap( getComparator() );
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentSortedMap( session, (java.util.SortedMap) collection );
	}
}
