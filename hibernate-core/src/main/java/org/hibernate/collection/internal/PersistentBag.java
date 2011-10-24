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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * An unordered, unkeyed collection that can contain the same element
 * multiple times. The Java collections API, curiously, has no <tt>Bag</tt>.
 * Most developers seem to use <tt>List</tt>s to represent bag semantics,
 * so Hibernate follows this practice.
 *
 * @author Gavin King
 */
public class PersistentBag extends AbstractPersistentCollection implements List {

	protected List bag;

	public PersistentBag(SessionImplementor session) {
		super(session);
	}

	public PersistentBag(SessionImplementor session, Collection coll) {
		super(session);
		if (coll instanceof List) {
			bag = (List) coll;
		}
		else {
			bag = new ArrayList();
			Iterator iter = coll.iterator();
			while ( iter.hasNext() ) {
				bag.add( iter.next() );
			}
		}
		setInitialized();
		setDirectlyAccessible(true);
	}

	public PersistentBag() {} //needed for SOAP libraries, etc

	public boolean isWrapper(Object collection) {
		return bag==collection;
	}
	public boolean empty() {
		return bag.isEmpty();
	}

	public Iterator entries(CollectionPersister persister) {
		return bag.iterator();
	}

	public Object readFrom(ResultSet rs, CollectionPersister persister, CollectionAliases descriptor, Object owner)
	throws HibernateException, SQLException {
		// note that if we load this collection from a cartesian product
		// the multiplicity would be broken ... so use an idbag instead
		Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() ) ;
		if (element!=null) bag.add(element);
		return element;
	}

	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
		this.bag = ( List ) persister.getCollectionType().instantiate( anticipatedSize );
	}

	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		Type elementType = persister.getElementType();
		List sn = (List) getSnapshot();
		if ( sn.size()!=bag.size() ) return false;
		for ( Object elt : bag ) {
			final boolean unequal = countOccurrences( elt, bag, elementType )
					!= countOccurrences( elt, sn, elementType );
			if ( unequal ) {
				return false;
			}
		}
		return true;
	}

	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Collection) snapshot ).isEmpty();
	}

	private int countOccurrences(Object element, List list, Type elementType)
	throws HibernateException {
		Iterator iter = list.iterator();
		int result=0;
		while ( iter.hasNext() ) {
			if ( elementType.isSame( element, iter.next() ) ) result++;
		}
		return result;
	}

	public Serializable getSnapshot(CollectionPersister persister)
	throws HibernateException {
		ArrayList clonedList = new ArrayList( bag.size() );
		Iterator iter = bag.iterator();
		while ( iter.hasNext() ) {
			clonedList.add( persister.getElementType().deepCopy( iter.next(), persister.getFactory() ) );
		}
		return clonedList;
	}

	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
	    List sn = (List) snapshot;
	    return getOrphans( sn, bag, entityName, getSession() );
	}


	public Serializable disassemble(CollectionPersister persister)
	throws HibernateException {

		int length = bag.size();
		Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementType().disassemble( bag.get(i), getSession(), null );
		}
		return result;
	}

	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
	throws HibernateException {
		Serializable[] array = (Serializable[]) disassembled;
		int size = array.length;
		beforeInitialize( persister, size );
		for ( int i = 0; i < size; i++ ) {
			Object element = persister.getElementType().assemble( array[i], getSession(), owner );
			if ( element!=null ) {
				bag.add( element );
			}
		}
	}

	public boolean needsRecreate(CollectionPersister persister) {
		return !persister.isOneToMany();
	}


	// For a one-to-many, a <bag> is not really a bag;
	// it is *really* a set, since it can't contain the
	// same element twice. It could be considered a bug
	// in the mapping dtd that <bag> allows <one-to-many>.

	// Anyway, here we implement <set> semantics for a
	// <one-to-many> <bag>!

	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		//if ( !persister.isOneToMany() ) throw new AssertionFailure("Not implemented for Bags");
		Type elementType = persister.getElementType();
		ArrayList deletes = new ArrayList();
		List sn = (List) getSnapshot();
		Iterator olditer = sn.iterator();
		int i=0;
		while ( olditer.hasNext() ) {
			Object old = olditer.next();
			Iterator newiter = bag.iterator();
			boolean found = false;
			if ( bag.size()>i && elementType.isSame( old, bag.get(i++) ) ) {
			//a shortcut if its location didn't change!
				found = true;
			}
			else {
				//search for it
				//note that this code is incorrect for other than one-to-many
				while ( newiter.hasNext() ) {
					if ( elementType.isSame( old, newiter.next() ) ) {
						found = true;
						break;
					}
				}
			}
			if (!found) deletes.add(old);
		}
		return deletes.iterator();
	}

	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		//if ( !persister.isOneToMany() ) throw new AssertionFailure("Not implemented for Bags");
		List sn = (List) getSnapshot();
		if ( sn.size()>i && elemType.isSame( sn.get(i), entry ) ) {
		//a shortcut if its location didn't change!
			return false;
		}
		else {
			//search for it
			//note that this code is incorrect for other than one-to-many
			Iterator olditer = sn.iterator();
			while ( olditer.hasNext() ) {
				Object old = olditer.next();
				if ( elemType.isSame( old, entry ) ) return false;
			}
			return true;
		}
	}

	public boolean isRowUpdatePossible() {
		return false;
	}

	public boolean needsUpdating(Object entry, int i, Type elemType) {
		//if ( !persister.isOneToMany() ) throw new AssertionFailure("Not implemented for Bags");
		return false;
	}

	/**
	 * @see java.util.Collection#size()
	 */
	public int size() {
		return readSize() ? getCachedSize() : bag.size();
	}

	/**
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : bag.isEmpty();
	}

	/**
	 * @see java.util.Collection#contains(Object)
	 */
	public boolean contains(Object object) {
		Boolean exists = readElementExistence(object);
		return exists==null ?
				bag.contains(object) :
				exists.booleanValue();
	}

	/**
	 * @see java.util.Collection#iterator()
	 */
	public Iterator iterator() {
		read();
		return new IteratorProxy( bag.iterator() );
	}

	/**
	 * @see java.util.Collection#toArray()
	 */
	public Object[] toArray() {
		read();
		return bag.toArray();
	}

	/**
	 * @see java.util.Collection#toArray(Object[])
	 */
	public Object[] toArray(Object[] a) {
		read();
		return bag.toArray(a);
	}

	/**
	 * @see java.util.Collection#add(Object)
	 */
	public boolean add(Object object) {
		if ( !isOperationQueueEnabled() ) {
			write();
			return bag.add(object);
		}
		else {
			queueOperation( new SimpleAdd(object) );
			return true;
		}
	}

	/**
	 * @see java.util.Collection#remove(Object)
	 */
	public boolean remove(Object o) {
		initialize( true );
		if ( bag.remove( o ) ) {
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * @see java.util.Collection#containsAll(Collection)
	 */
	public boolean containsAll(Collection c) {
		read();
		return bag.containsAll(c);
	}

	/**
	 * @see java.util.Collection#addAll(Collection)
	 */
	public boolean addAll(Collection values) {
		if ( values.size()==0 ) return false;
		if ( !isOperationQueueEnabled() ) {
			write();
			return bag.addAll(values);
		}
		else {
			Iterator iter = values.iterator();
			while ( iter.hasNext() ) {
				queueOperation( new SimpleAdd( iter.next() ) );
			}
			return values.size()>0;
		}
	}

	/**
	 * @see java.util.Collection#removeAll(Collection)
	 */
	public boolean removeAll(Collection c) {
		if ( c.size()>0 ) {
			initialize( true );
			if ( bag.removeAll( c ) ) {
				dirty();
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}

	/**
	 * @see java.util.Collection#retainAll(Collection)
	 */
	public boolean retainAll(Collection c) {
		initialize( true );
		if ( bag.retainAll( c ) ) {
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		if ( isClearQueueEnabled() ) {
			queueOperation( new Clear() );
		}
		else {
			initialize( true );
			if ( ! bag.isEmpty() ) {
				bag.clear();
				dirty();
			}
		}
	}

	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		throw new UnsupportedOperationException("Bags don't have indexes");
	}

	public Object getElement(Object entry) {
		return entry;
	}

	public Object getSnapshotElement(Object entry, int i) {
		List sn = (List) getSnapshot();
		return sn.get(i);
	}

	public int occurrences(Object o) {
		read();
		Iterator iter = bag.iterator();
		int result=0;
		while ( iter.hasNext() ) {
			if ( o.equals( iter.next() ) ) result++;
		}
		return result;
	}

	// List OPERATIONS:

	/**
	 * @see java.util.List#add(int, Object)
	 */
	public void add(int i, Object o) {
		write();
		bag.add(i, o);
	}

	/**
	 * @see java.util.List#addAll(int, Collection)
	 */
	public boolean addAll(int i, Collection c) {
		if ( c.size()>0 ) {
			write();
			return bag.addAll(i, c);
		}
		else {
			return false;
		}
	}

	/**
	 * @see java.util.List#get(int)
	 */
	public Object get(int i) {
		read();
		return bag.get(i);
	}

	/**
	 * @see java.util.List#indexOf(Object)
	 */
	public int indexOf(Object o) {
		read();
		return bag.indexOf(o);
	}

	/**
	 * @see java.util.List#lastIndexOf(Object)
	 */
	public int lastIndexOf(Object o) {
		read();
		return bag.lastIndexOf(o);
	}

	/**
	 * @see java.util.List#listIterator()
	 */
	public ListIterator listIterator() {
		read();
		return new ListIteratorProxy( bag.listIterator() );
	}

	/**
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator listIterator(int i) {
		read();
		return new ListIteratorProxy( bag.listIterator(i) );
	}

	/**
	 * @see java.util.List#remove(int)
	 */
	public Object remove(int i) {
		write();
		return bag.remove(i);
	}

	/**
	 * @see java.util.List#set(int, Object)
	 */
	public Object set(int i, Object o) {
		write();
		return bag.set(i, o);
	}

	/**
	 * @see java.util.List#subList(int, int)
	 */
	public List subList(int start, int end) {
		read();
		return new ListProxy( bag.subList(start, end) );
	}

	public String toString() {
		read();
		return bag.toString();
	}

	/*public boolean equals(Object other) {
		read();
		return bag.equals(other);
	}

	public int hashCode(Object other) {
		read();
		return bag.hashCode();
	}*/

	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

	/**
	 * Bag does not respect the collection API and do an
	 * JVM instance comparison to do the equals.
	 * The semantic is broken not to have to initialize a
	 * collection for a simple equals() operation.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return super.hashCode();
	}

	final class Clear implements DelayedOperation {
		public void operate() {
			bag.clear();
		}
		public Object getAddedInstance() {
			return null;
		}
		public Object getOrphan() {
			throw new UnsupportedOperationException("queued clear cannot be used with orphan delete");
		}
	}

	final class SimpleAdd implements DelayedOperation {
		private Object value;

		public SimpleAdd(Object value) {
			this.value = value;
		}
		public void operate() {
			bag.add(value);
		}
		public Object getAddedInstance() {
			return value;
		}
		public Object getOrphan() {
			return null;
		}
	}

}
