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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

public class SortedSetType extends SetType {
	private final Comparator comparator;

	/**
	 * @deprecated Use {@link #SortedSetType(org.hibernate.type.TypeFactory.TypeScope, String, String, java.util.Comparator)}
	 * instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public SortedSetType(TypeFactory.TypeScope typeScope, String role, String propertyRef, Comparator comparator, boolean isEmbeddedInXML) {
		super( typeScope, role, propertyRef, isEmbeddedInXML );
		this.comparator = comparator;
	}

	public SortedSetType(TypeFactory.TypeScope typeScope, String role, String propertyRef, Comparator comparator) {
		super( typeScope, role, propertyRef );
		this.comparator = comparator;
	}

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) {
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
	
	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new PersistentSortedSet( session, (java.util.SortedSet) collection );
	}
}
