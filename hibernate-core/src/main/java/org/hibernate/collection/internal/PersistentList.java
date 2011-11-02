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

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * A persistent wrapper for a <tt>java.util.List</tt>. Underlying
 * collection is an <tt>ArrayList</tt>.
 *
 * @see java.util.ArrayList
 * @author Gavin King
 */
public class PersistentList extends AbstractPersistentCollection implements List {
	protected List list;

	@Override
	@SuppressWarnings( {"unchecked"})
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		final EntityMode entityMode = persister.getOwnerEntityPersister().getEntityMode();

		ArrayList clonedList = new ArrayList( list.size() );
		for ( Object element : list ) {
			Object deepCopy = persister.getElementType().deepCopy( element, persister.getFactory() );
			clonedList.add( deepCopy );
		}
		return clonedList;
	}

	@Override
	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		List sn = (List) snapshot;
	    return getOrphans( sn, list, entityName, getSession() );
	}

	@Override
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		Type elementType = persister.getElementType();
		List sn = (List) getSnapshot();
		if ( sn.size()!=this.list.size() ) return false;
		Iterator iter = list.iterator();
		Iterator sniter = sn.iterator();
		while ( iter.hasNext() ) {
			if ( elementType.isDirty( iter.next(), sniter.next(), getSession() ) ) return false;
		}
		return true;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Collection) snapshot ).isEmpty();
	}

	public PersistentList(SessionImplementor session) {
		super(session);
	}

	public PersistentList(SessionImplementor session, List list) {
		super(session);
		this.list = list;
		setInitialized();
		setDirectlyAccessible(true);
	}

	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
		this.list = ( List ) persister.getCollectionType().instantiate( anticipatedSize );
	}

	public boolean isWrapper(Object collection) {
		return list==collection;
	}

	public PersistentList() {} //needed for SOAP libraries, etc

	/**
	 * @see java.util.List#size()
	 */
	public int size() {
		return readSize() ? getCachedSize() : list.size();
	}

	/**
	 * @see java.util.List#isEmpty()
	 */
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : list.isEmpty();
	}

	/**
	 * @see java.util.List#contains(Object)
	 */
	public boolean contains(Object object) {
		Boolean exists = readElementExistence(object);
		return exists==null ?
				list.contains(object) :
				exists.booleanValue();
	}

	/**
	 * @see java.util.List#iterator()
	 */
	public Iterator iterator() {
		read();
		return new IteratorProxy( list.iterator() );
	}

	/**
	 * @see java.util.List#toArray()
	 */
	public Object[] toArray() {
		read();
		return list.toArray();
	}

	/**
	 * @see java.util.List#toArray(Object[])
	 */
	public Object[] toArray(Object[] array) {
		read();
		return list.toArray(array);
	}

	/**
	 * @see java.util.List#add(Object)
	 */
	public boolean add(Object object) {
		if ( !isOperationQueueEnabled() ) {
			write();
			return list.add(object);
		}
		else {
			queueOperation( new SimpleAdd(object) );
			return true;
		}
	}

	/**
	 * @see java.util.List#remove(Object)
	 */
	public boolean remove(Object value) {
		Boolean exists = isPutQueueEnabled() ? readElementExistence(value) : null;
		if ( exists == null ) {
			initialize( true );
			if ( list.remove( value ) ) {
				dirty();
				return true;
			}
			else {
				return false;
			}
		}
		else if ( exists.booleanValue() ) {
			queueOperation( new SimpleRemove(value) );
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * @see java.util.List#containsAll(Collection)
	 */
	public boolean containsAll(Collection coll) {
		read();
		return list.containsAll(coll);
	}

	/**
	 * @see java.util.List#addAll(Collection)
	 */
	public boolean addAll(Collection values) {
		if ( values.size()==0 ) {
			return false;
		}
		if ( !isOperationQueueEnabled() ) {
			write();
			return list.addAll(values);
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
	 * @see java.util.List#addAll(int, Collection)
	 */
	public boolean addAll(int index, Collection coll) {
		if ( coll.size()>0 ) {
			write();
			return list.addAll(index,  coll);
		}
		else {
			return false;
		}
	}

	/**
	 * @see java.util.List#removeAll(Collection)
	 */
	public boolean removeAll(Collection coll) {
		if ( coll.size()>0 ) {
			initialize( true );
			if ( list.removeAll( coll ) ) {
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
	 * @see java.util.List#retainAll(Collection)
	 */
	public boolean retainAll(Collection coll) {
		initialize( true );
		if ( list.retainAll( coll ) ) {
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * @see java.util.List#clear()
	 */
	public void clear() {
		if ( isClearQueueEnabled() ) {
			queueOperation( new Clear() );
		}
		else {
			initialize( true );
			if ( ! list.isEmpty() ) {
				list.clear();
				dirty();
			}
		}
	}

	/**
	 * @see java.util.List#get(int)
	 */
	public Object get(int index) {
		if (index<0) {
			throw new ArrayIndexOutOfBoundsException("negative index");
		}
		Object result = readElementByIndex( index );
		return result==UNKNOWN ? list.get(index) : result;
	}

	/**
	 * @see java.util.List#set(int, Object)
	 */
	public Object set(int index, Object value) {
		if (index<0) {
			throw new ArrayIndexOutOfBoundsException("negative index");
		}
		Object old = isPutQueueEnabled() ? readElementByIndex( index ) : UNKNOWN;
		if ( old==UNKNOWN ) {
			write();
			return list.set(index, value);
		}
		else {
			queueOperation( new Set(index, value, old) );
			return old;
		}
	}

	/**
	 * @see java.util.List#add(int, Object)
	 */
	public void add(int index, Object value) {
		if (index<0) {
			throw new ArrayIndexOutOfBoundsException("negative index");
		}
		if ( !isOperationQueueEnabled() ) {
			write();
			list.add(index, value);
		}
		else {
			queueOperation( new Add(index, value) );
		}
	}

	/**
	 * @see java.util.List#remove(int)
	 */
	public Object remove(int index) {
		if (index<0) {
			throw new ArrayIndexOutOfBoundsException("negative index");
		}
		Object old = isPutQueueEnabled() ?
				readElementByIndex( index ) : UNKNOWN;
		if ( old==UNKNOWN ) {
			write();
			return list.remove(index);
		}
		else {
			queueOperation( new Remove(index, old) );
			return old;
		}
	}

	/**
	 * @see java.util.List#indexOf(Object)
	 */
	public int indexOf(Object value) {
		read();
		return list.indexOf(value);
	}

	/**
	 * @see java.util.List#lastIndexOf(Object)
	 */
	public int lastIndexOf(Object value) {
		read();
		return list.lastIndexOf(value);
	}

	/**
	 * @see java.util.List#listIterator()
	 */
	public ListIterator listIterator() {
		read();
		return new ListIteratorProxy( list.listIterator() );
	}

	/**
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator listIterator(int index) {
		read();
		return new ListIteratorProxy( list.listIterator(index) );
	}

	/**
	 * @see java.util.List#subList(int, int)
	 */
	public java.util.List subList(int from, int to) {
		read();
		return new ListProxy( list.subList(from, to) );
	}

	public boolean empty() {
		return list.isEmpty();
	}

	public String toString() {
		read();
		return list.toString();
	}

	public Object readFrom(ResultSet rs, CollectionPersister persister, CollectionAliases descriptor, Object owner)
	throws HibernateException, SQLException {
		Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() ) ;
		int index = ( (Integer) persister.readIndex( rs, descriptor.getSuffixedIndexAliases(), getSession() ) ).intValue();

		//pad with nulls from the current last element up to the new index
		for ( int i = list.size(); i<=index; i++) {
			list.add(i, null);
		}

		list.set(index, element);
		return element;
	}

	public Iterator entries(CollectionPersister persister) {
		return list.iterator();
	}

	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
	throws HibernateException {
		Serializable[] array = ( Serializable[] ) disassembled;
		int size = array.length;
		beforeInitialize( persister, size );
		for ( int i = 0; i < size; i++ ) {
			list.add( persister.getElementType().assemble( array[i], getSession(), owner ) );
		}
	}

	public Serializable disassemble(CollectionPersister persister)
	throws HibernateException {

		int length = list.size();
		Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementType().disassemble( list.get(i), getSession(), null );
		}
		return result;
	}


	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		List deletes = new ArrayList();
		List sn = (List) getSnapshot();
		int end;
		if ( sn.size() > list.size() ) {
			for ( int i=list.size(); i<sn.size(); i++ ) {
				deletes.add( indexIsFormula ? sn.get(i) : i );
			}
			end = list.size();
		}
		else {
			end = sn.size();
		}
		for ( int i=0; i<end; i++ ) {
			if ( list.get(i)==null && sn.get(i)!=null ) {
				deletes.add( indexIsFormula ? sn.get(i) : i );
			}
		}
		return deletes.iterator();
	}

	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		final List sn = (List) getSnapshot();
		return list.get(i)!=null && ( i >= sn.size() || sn.get(i)==null );
	}

	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {
		final List sn = (List) getSnapshot();
		return i<sn.size() && sn.get(i)!=null && list.get(i)!=null &&
			elemType.isDirty( list.get(i), sn.get(i), getSession() );
	}

	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		return i;
	}

	public Object getElement(Object entry) {
		return entry;
	}

	public Object getSnapshotElement(Object entry, int i) {
		final List sn = (List) getSnapshot();
		return sn.get(i);
	}

	public boolean equals(Object other) {
		read();
		return list.equals(other);
	}

	public int hashCode() {
		read();
		return list.hashCode();
	}

	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

	final class Clear implements DelayedOperation {
		public void operate() {
			list.clear();
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
			list.add(value);
		}
		public Object getAddedInstance() {
			return value;
		}
		public Object getOrphan() {
			return null;
		}
	}

	final class Add implements DelayedOperation {
		private int index;
		private Object value;

		public Add(int index, Object value) {
			this.index = index;
			this.value = value;
		}
		public void operate() {
			list.add(index, value);
		}
		public Object getAddedInstance() {
			return value;
		}
		public Object getOrphan() {
			return null;
		}
	}

	final class Set implements DelayedOperation {
		private int index;
		private Object value;
		private Object old;

		public Set(int index, Object value, Object old) {
			this.index = index;
			this.value = value;
			this.old = old;
		}
		public void operate() {
			list.set(index, value);
		}
		public Object getAddedInstance() {
			return value;
		}
		public Object getOrphan() {
			return old;
		}
	}

	final class Remove implements DelayedOperation {
		private int index;
		private Object old;

		public Remove(int index, Object old) {
			this.index = index;
			this.old = old;
		}
		public void operate() {
			list.remove(index);
		}
		public Object getAddedInstance() {
			return null;
		}
		public Object getOrphan() {
			return old;
		}
	}

	final class SimpleRemove implements DelayedOperation {
		private Object value;

		public SimpleRemove(Object value) {
			this.value = value;
		}
		public void operate() {
			list.remove(value);
		}
		public Object getAddedInstance() {
			return null;
		}
		public Object getOrphan() {
			return value;
		}
	}
}
