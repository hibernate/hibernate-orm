/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeMap;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;

/**
 * A persistent wrapper for a <tt>java.util.SortedSet</tt>. Underlying
 * collection is a <tt>TreeSet</tt>.
 *
 * @see java.util.TreeSet
 * @author <a href="mailto:doug.currie@alum.mit.edu">e</a>
 */
public class PersistentSortedSet extends PersistentSet implements SortedSet {
	protected Comparator comparator;

	/**
	 * Constructs a PersistentSortedSet.  This form needed for SOAP libraries, etc
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PersistentSortedSet() {
	}

	/**
	 * Constructs a PersistentSortedSet
	 *
	 * @param session The session
	 */
	public PersistentSortedSet(SessionImplementor session) {
		super( session );
	}

	/**
	 * Constructs a PersistentSortedSet
	 *
	 * @param session The session
	 * @param set The underlying set data
	 */
	public PersistentSortedSet(SessionImplementor session, SortedSet set) {
		super( session, set );
		comparator = set.comparator();
	}

	@SuppressWarnings({"unchecked", "UnusedParameters"})
	protected Serializable snapshot(BasicCollectionPersister persister, EntityMode entityMode)
			throws HibernateException {
		final TreeMap clonedSet = new TreeMap( comparator );
		for ( Object setElement : set ) {
			final Object copy = persister.getElementType().deepCopy( setElement, persister.getFactory() );
			clonedSet.put( copy, copy );
		}
		return clonedSet;
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	@Override
	public Comparator comparator() {
		return comparator;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SortedSet subSet(Object fromElement, Object toElement) {
		read();
		final SortedSet subSet = ( (SortedSet) set ).subSet( fromElement, toElement );
		return new SubSetProxy( subSet );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SortedSet headSet(Object toElement) {
		read();
		final SortedSet headSet = ( (SortedSet) set ).headSet( toElement );
		return new SubSetProxy( headSet );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SortedSet tailSet(Object fromElement) {
		read();
		final SortedSet tailSet = ( (SortedSet) set ).tailSet( fromElement );
		return new SubSetProxy( tailSet );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object first() {
		read();
		return ( (SortedSet) set ).first();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object last() {
		read();
		return ( (SortedSet) set ).last();
	}

	/**
	 * wrapper for subSets to propagate write to its backing set
	 */
	class SubSetProxy extends SetProxy implements SortedSet {
		SubSetProxy(SortedSet s) {
			super( s );
		}

		@Override
		@SuppressWarnings("unchecked")
		public Comparator comparator() {
			return ( (SortedSet) this.set ).comparator();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object first() {
			return ( (SortedSet) this.set ).first();
		}

		@Override
		@SuppressWarnings("unchecked")
		public SortedSet headSet(Object toValue) {
			return new SubSetProxy( ( (SortedSet) this.set ).headSet( toValue ) );
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object last() {
			return ( (SortedSet) this.set ).last();
		}

		@Override
		@SuppressWarnings("unchecked")
		public SortedSet subSet(Object fromValue, Object toValue) {
			return new SubSetProxy( ( (SortedSet) this.set ).subSet( fromValue, toValue ) );
		}

		@Override
		@SuppressWarnings("unchecked")
		public SortedSet tailSet(Object fromValue) {
			return new SubSetProxy( ( (SortedSet) this.set ).tailSet( fromValue ) );
		}
	}
}
