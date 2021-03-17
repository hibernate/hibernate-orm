/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Comparator;
import java.util.TreeSet;

import org.hibernate.collection.internal.PersistentSortedSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

public class SortedSetType extends SetType {

	private final Comparator comparator;

	/**
	 * @deprecated Use the other constructor
	 */
	@Deprecated
	public SortedSetType(TypeFactory.TypeScope typeScope, String role, String propertyRef, Comparator comparator) {
		this( role, propertyRef, comparator );
	}

	public SortedSetType(String role, String propertyRef, Comparator comparator) {
		super( role, propertyRef );
		this.comparator = comparator;
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Serializable key) {
		PersistentSortedSet set = new PersistentSortedSet(session);
		set.setComparator(comparator);
		return set;
	}

	public Class getReturnedClass() {
		return java.util.SortedSet.class;
	}

	@SuppressWarnings( {"unchecked"})
	public Object instantiate(int anticipatedSize) {
		return new TreeSet(comparator);
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentSortedSet( session, (java.util.SortedSet) collection );
	}
}
