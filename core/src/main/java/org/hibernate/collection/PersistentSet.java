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
package org.hibernate.collection;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;


/**
 * A persistent wrapper for a <tt>java.util.Set</tt>. The underlying
 * collection is a <tt>HashSet</tt>.
 *
 * @see java.util.HashSet
 * @author Gavin King
 */
public class PersistentSet extends AbstractPersistentCollection implements java.util.Set {

	protected Set set;
	protected transient List tempList;

	/**
	 * Empty constructor.
	 * <p/>
	 * Note: this form is not ever ever ever used by Hibernate; it is, however,
	 * needed for SOAP libraries and other such marshalling code.
	 */
	public PersistentSet() {
		// intentionally empty
	}

	/**
	 * Constructor matching super.  Instantiates a lazy set (the underlying
	 * set is un-initialized).
	 *
	 * @param session The session to which this set will belong.
	 */
	public PersistentSet(SessionImplementor session) {
		super( session );
	}

	/**
	 * Instantiates a non-lazy set (the underlying set is constructed
	 * from the incoming set reference).
	 *
	 * @param session The session to which this set will belong.
	 * @param set The underlying set data.
	 */
	public PersistentSet(SessionImplementor session, java.util.Set set) {
		super(session);
		// Sets can be just a view of a part of another collection.
		// do we need to copy it to be sure it won't be changing
		// underneath us?
		// ie. this.set.addAll(set);
		this.set = set;
		setInitialized();
		setDirectlyAccessible(true);
	}


	public Serializable getSnapshot(CollectionPersister persister) 
	throws HibernateException {
		EntityMode entityMode = getSession().getEntityMode();
		
		//if (set==null) return new Set(session);
		HashMap clonedSet = new HashMap( set.size() );
		Iterator iter = set.iterator();
		while ( iter.hasNext() ) {
			Object copied = persister.getElementType()
					.deepCopy( iter.next(), entityMode, persister.getFactory() );
			clonedSet.put(copied, copied);
		}
		return clonedSet;
	}

	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		java.util.Map sn = (java.util.Map) snapshot;
		return getOrphans( sn.keySet(), set, entityName, getSession() );
	}

	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		Type elementType = persister.getElementType();
		java.util.Map sn = (java.util.Map) getSnapshot();
		if ( sn.size()!=set.size() ) {
			return false;
		}
		else {
			Iterator iter = set.iterator();
			while ( iter.hasNext() ) {
				Object test = iter.next();
				Object oldValue = sn.get(test);
				if ( oldValue==null || elementType.isDirty( oldValue, test, getSession() ) ) return false;
			}
			return true;
		}
	}

	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (java.util.Map) snapshot ).isEmpty();
	}

	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
		this.set = ( Set ) persister.getCollectionType().instantiate( anticipatedSize );
	}

	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
	throws HibernateException {
		Serializable[] array = ( Serializable[] ) disassembled;
		int size = array.length;
		beforeInitialize( persister, size );
		for (int i = 0; i < size; i++ ) {
			Object element = persister.getElementType().assemble( array[i], getSession(), owner );
			if ( element != null ) {
				set.add( element );
			}
		}
	}

	public boolean empty() {
		return set.isEmpty();
	}

	/**
	 * @see java.util.Set#size()
	 */
	public int size() {
		return readSize() ? getCachedSize() : set.size();
	}

	/**
	 * @see java.util.Set#isEmpty()
	 */
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : set.isEmpty();
	}

	/**
	 * @see java.util.Set#contains(Object)
	 */
	public boolean contains(Object object) {
		Boolean exists = readElementExistence(object);
		return exists==null ? 
				set.contains(object) : 
				exists.booleanValue();
	}

	/**
	 * @see java.util.Set#iterator()
	 */
	public Iterator iterator() {
		read();
		return new IteratorProxy( set.iterator() );
	}

	/**
	 * @see java.util.Set#toArray()
	 */
	public Object[] toArray() {
		read();
		return set.toArray();
	}

	/**
	 * @see java.util.Set#toArray(Object[])
	 */
	public Object[] toArray(Object[] array) {
		read();
		return set.toArray(array);
	}

	/**
	 * @see java.util.Set#add(Object)
	 */
	public boolean add(Object value) {
		Boolean exists = isOperationQueueEnabled() ? readElementExistence( value ) : null;
		if ( exists == null ) {
			initialize( true );
			if ( set.add( value ) ) {
				dirty();
				return true;
			}
			else {
				return false;
			}
		}
		else if ( exists.booleanValue() ) {
			return false;
		}
		else {
			queueOperation( new SimpleAdd(value) );
			return true;
		}
	}

	/**
	 * @see java.util.Set#remove(Object)
	 */
	public boolean remove(Object value) {
		Boolean exists = isPutQueueEnabled() ? readElementExistence( value ) : null;
		if ( exists==null ) {
			initialize( true );
			if ( set.remove( value ) ) {
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
	 * @see java.util.Set#containsAll(Collection)
	 */
	public boolean containsAll(Collection coll) {
		read();
		return set.containsAll(coll);
	}

	/**
	 * @see java.util.Set#addAll(Collection)
	 */
	public boolean addAll(Collection coll) {
		if ( coll.size() > 0 ) {
			initialize( true );
			if ( set.addAll( coll ) ) {
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
	 * @see java.util.Set#retainAll(Collection)
	 */
	public boolean retainAll(Collection coll) {
		initialize( true );
		if ( set.retainAll( coll ) ) {
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * @see java.util.Set#removeAll(Collection)
	 */
	public boolean removeAll(Collection coll) {
		if ( coll.size() > 0 ) {
			initialize( true );
			if ( set.removeAll( coll ) ) {
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
	 * @see java.util.Set#clear()
	 */
	public void clear() {
		if ( isClearQueueEnabled() ) {
			queueOperation( new Clear() );
		}
		else {
			initialize( true );
			if ( !set.isEmpty() ) {
				set.clear();
				dirty();
			}
		}
	}

	public String toString() {
		//if (needLoading) return "asleep";
		read();
		return set.toString();
	}

	public Object readFrom(
	        ResultSet rs,
	        CollectionPersister persister,
	        CollectionAliases descriptor,
	        Object owner) throws HibernateException, SQLException {
		Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
		if (element!=null) tempList.add(element);
		return element;
	}

	public void beginRead() {
		super.beginRead();
		tempList = new ArrayList();
	}

	public boolean endRead() {
		set.addAll(tempList);
		tempList = null;
		setInitialized();
		return true;
	}

	public Iterator entries(CollectionPersister persister) {
		return set.iterator();
	}

	public Serializable disassemble(CollectionPersister persister)
	throws HibernateException {

		Serializable[] result = new Serializable[ set.size() ];
		Iterator iter = set.iterator();
		int i=0;
		while ( iter.hasNext() ) {
			result[i++] = persister.getElementType().disassemble( iter.next(), getSession(), null );
		}
		return result;

	}

	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		Type elementType = persister.getElementType();
		final java.util.Map sn = (java.util.Map) getSnapshot();
		ArrayList deletes = new ArrayList( sn.size() );
		Iterator iter = sn.keySet().iterator();
		while ( iter.hasNext() ) {
			Object test = iter.next();
			if ( !set.contains(test) ) {
				// the element has been removed from the set
				deletes.add(test);
			}
		}
		iter = set.iterator();
		while ( iter.hasNext() ) {
			Object test = iter.next();
			Object oldValue = sn.get(test);
			if ( oldValue!=null && elementType.isDirty( test, oldValue, getSession() ) ) {
				// the element has changed
				deletes.add(oldValue);
			}
		}
		return deletes.iterator();
	}

	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		final java.util.Map sn = (java.util.Map) getSnapshot();
		Object oldValue = sn.get(entry);
		// note that it might be better to iterate the snapshot but this is safe,
		// assuming the user implements equals() properly, as required by the Set
		// contract!
		return oldValue==null || elemType.isDirty( oldValue, entry, getSession() );
	}

	public boolean needsUpdating(Object entry, int i, Type elemType) {
		return false;
	}

	public boolean isRowUpdatePossible() {
		return false;
	}

	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		throw new UnsupportedOperationException("Sets don't have indexes");
	}

	public Object getElement(Object entry) {
		return entry;
	}

	public Object getSnapshotElement(Object entry, int i) {
		throw new UnsupportedOperationException("Sets don't support updating by element");
	}

	public boolean equals(Object other) {
		read();
		return set.equals(other);
	}

	public int hashCode() {
		read();
		return set.hashCode();
	}

	public boolean entryExists(Object key, int i) {
		return true;
	}

	public boolean isWrapper(Object collection) {
		return set==collection;
	}

	final class Clear implements DelayedOperation {
		public void operate() {
			set.clear();
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
			set.add(value);
		}
		public Object getAddedInstance() {
			return value;
		}
		public Object getOrphan() {
			return null;
		}
	}

	final class SimpleRemove implements DelayedOperation {
		private Object value;
		
		public SimpleRemove(Object value) {
			this.value = value;
		}
		public void operate() {
			set.remove(value);
		}
		public Object getAddedInstance() {
			return null;
		}
		public Object getOrphan() {
			return value;
		}
	}
}
