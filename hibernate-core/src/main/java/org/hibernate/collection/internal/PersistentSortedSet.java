/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
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

	protected Serializable snapshot(BasicCollectionPersister persister, EntityMode entityMode) 
	throws HibernateException {
		//if (set==null) return new Set(session);
		TreeMap clonedSet = new TreeMap(comparator);
		Iterator iter = set.iterator();
		while ( iter.hasNext() ) {
			Object copy = persister.getElementType().deepCopy( iter.next(), persister.getFactory() );
			clonedSet.put(copy, copy);
		}
		return clonedSet;
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	public PersistentSortedSet(SessionImplementor session) {
		super(session);
	}

	public PersistentSortedSet(SessionImplementor session, SortedSet set) {
		super(session, set);
		comparator = set.comparator();
	}

	public PersistentSortedSet() {} //needed for SOAP libraries, etc

	/**
	 * @see PersistentSortedSet#comparator()
	 */
	public Comparator comparator() {
		return comparator;
	}

	/**
	 * @see PersistentSortedSet#subSet(Object,Object)
	 */
	public SortedSet subSet(Object fromElement, Object toElement) {
		read();
		SortedSet s;
		s = ( (SortedSet) set ).subSet(fromElement, toElement);
		return new SubSetProxy(s);
	}

	/**
	 * @see PersistentSortedSet#headSet(Object)
	 */
	public SortedSet headSet(Object toElement) {
		read();
		SortedSet s = ( (SortedSet) set ).headSet(toElement);
		return new SubSetProxy(s);
	}

	/**
	 * @see PersistentSortedSet#tailSet(Object)
	 */
	public SortedSet tailSet(Object fromElement) {
		read();
		SortedSet s = ( (SortedSet) set ).tailSet(fromElement);
		return new SubSetProxy(s);
	}

	/**
	 * @see PersistentSortedSet#first()
	 */
	public Object first() {
		read();
		return ( (SortedSet) set ).first();
	}

	/**
	 * @see PersistentSortedSet#last()
	 */
	public Object last() {
		read();
		return ( (SortedSet) set ).last();
	}

	/** wrapper for subSets to propagate write to its backing set */
	class SubSetProxy extends SetProxy implements SortedSet {

		SubSetProxy(SortedSet s) {
			super(s);
		}

		public Comparator comparator() {
			return ( (SortedSet) this.set ).comparator();
		}

		public Object first() {
			return ( (SortedSet) this.set ).first();
		}

		public SortedSet headSet(Object toValue) {
			return new SubSetProxy( ( (SortedSet) this.set ).headSet(toValue) );
		}

		public Object last() {
			return ( (SortedSet) this.set ).last();
		}

		public SortedSet subSet(Object fromValue, Object toValue) {
			return new SubSetProxy( ( (SortedSet) this.set ).subSet(fromValue, toValue) );
		}

		public SortedSet tailSet(Object fromValue) {
			return new SubSetProxy( ( (SortedSet) this.set ).tailSet(fromValue) );
		}

	}

}







