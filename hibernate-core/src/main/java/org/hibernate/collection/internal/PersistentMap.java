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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;


/**
 * A persistent wrapper for a <tt>java.util.Map</tt>. Underlying collection
 * is a <tt>HashMap</tt>.
 *
 * @see java.util.HashMap
 * @author Gavin King
 */
public class PersistentMap extends AbstractPersistentCollection implements Map {

	protected Map map;

	/**
	 * Empty constructor.
	 * <p/>
	 * Note: this form is not ever ever ever used by Hibernate; it is, however,
	 * needed for SOAP libraries and other such marshalling code.
	 */
	public PersistentMap() {
		// intentionally empty
	}

	/**
	 * Instantiates a lazy map (the underlying map is un-initialized).
	 *
	 * @param session The session to which this map will belong.
	 */
	public PersistentMap(SessionImplementor session) {
		super(session);
	}

	/**
	 * Instantiates a non-lazy map (the underlying map is constructed
	 * from the incoming map reference).
	 *
	 * @param session The session to which this map will belong.
	 * @param map The underlying map data.
	 */
	public PersistentMap(SessionImplementor session, Map map) {
		super(session);
		this.map = map;
		setInitialized();
		setDirectlyAccessible(true);
	}

	@SuppressWarnings( {"unchecked"})
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		HashMap clonedMap = new HashMap( map.size() );
		for ( Object o : map.entrySet() ) {
			Entry e = (Entry) o;
			final Object copy = persister.getElementType().deepCopy( e.getValue(), persister.getFactory() );
			clonedMap.put( e.getKey(), copy );
		}
		return clonedMap;
	}

	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		Map sn = (Map) snapshot;
		return getOrphans( sn.values(), map.values(), entityName, getSession() );
	}

	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		Type elementType = persister.getElementType();
		Map xmap = (Map) getSnapshot();
		if ( xmap.size()!=this.map.size() ) return false;
		Iterator iter = map.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry entry = (Map.Entry) iter.next();
			if ( elementType.isDirty( entry.getValue(), xmap.get( entry.getKey() ), getSession() ) ) return false;
		}
		return true;
	}

	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Map) snapshot ).isEmpty();
	}

	public boolean isWrapper(Object collection) {
		return map==collection;
	}

	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
		this.map = ( Map ) persister.getCollectionType().instantiate( anticipatedSize );
	}


	/**
	 * @see java.util.Map#size()
	 */
	public int size() {
		return readSize() ? getCachedSize() : map.size();
	}

	/**
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : map.isEmpty();
	}

	/**
	 * @see java.util.Map#containsKey(Object)
	 */
	public boolean containsKey(Object key) {
		Boolean exists = readIndexExistence(key);
		return exists==null ? map.containsKey(key) : exists.booleanValue();
	}

	/**
	 * @see java.util.Map#containsValue(Object)
	 */
	public boolean containsValue(Object value) {
		Boolean exists = readElementExistence(value);
		return exists==null ? 
				map.containsValue(value) : 
				exists.booleanValue();
	}

	/**
	 * @see java.util.Map#get(Object)
	 */
	public Object get(Object key) {
		Object result = readElementByIndex(key);
		return result==UNKNOWN ? map.get(key) : result;
	}

	/**
	 * @see java.util.Map#put(Object, Object)
	 */
	public Object put(Object key, Object value) {
		if ( isPutQueueEnabled() ) {
			Object old = readElementByIndex( key );
			if ( old != UNKNOWN ) {
				queueOperation( new Put( key, value, old ) );
				return old;
			}
		}
		initialize( true );
		Object old = map.put( key, value );
		// would be better to use the element-type to determine
		// whether the old and the new are equal here; the problem being
		// we do not necessarily have access to the element type in all
		// cases
		if ( value != old ) {
			dirty();
		}
		return old;
	}

	/**
	 * @see java.util.Map#remove(Object)
	 */
	public Object remove(Object key) {
		if ( isPutQueueEnabled() ) {
			Object old = readElementByIndex( key );
			if ( old != UNKNOWN ) {
				queueOperation( new Remove( key, old ) );
				return old;
			}
		}
		// TODO : safe to interpret "map.remove(key) == null" as non-dirty?
		initialize( true );
		if ( map.containsKey( key ) ) {
			dirty();
		}
		return map.remove( key );
	}

	/**
	 * @see java.util.Map#putAll(java.util.Map puts)
	 */
	public void putAll(Map puts) {
		if ( puts.size()>0 ) {
			initialize( true );
			Iterator itr = puts.entrySet().iterator();
			while ( itr.hasNext() ) {
				Map.Entry entry = ( Entry ) itr.next();
				put( entry.getKey(), entry.getValue() );
			}
		}
	}

	/**
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		if ( isClearQueueEnabled() ) {
			queueOperation( new Clear() );
		}
		else {
			initialize( true );
			if ( ! map.isEmpty() ) {
				dirty();
				map.clear();
			}
		}
	}

	/**
	 * @see java.util.Map#keySet()
	 */
	public Set keySet() {
		read();
		return new SetProxy( map.keySet() );
	}

	/**
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		read();
		return new SetProxy( map.values() );
	}

	/**
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet() {
		read();
		return new EntrySetProxy( map.entrySet() );
	}

	public boolean empty() {
		return map.isEmpty();
	}

	public String toString() {
		read();
		return map.toString();
	}

	private transient List<Object[]> loadingEntries;

	public Object readFrom(
			ResultSet rs,
			CollectionPersister persister,
			CollectionAliases descriptor,
			Object owner) throws HibernateException, SQLException {
		final Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
		if ( element != null ) {
			final Object index = persister.readIndex( rs, descriptor.getSuffixedIndexAliases(), getSession() );
			if ( loadingEntries == null ) {
				loadingEntries = new ArrayList<Object[]>();
			}
			loadingEntries.add( new Object[] { index, element } );
		}
		return element;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean endRead() {
		if ( loadingEntries != null ) {
			for ( Object[] entry : loadingEntries ) {
				map.put( entry[0], entry[1] );
			}
            loadingEntries = null;
		}
		return super.endRead();
	}

	public Iterator entries(CollectionPersister persister) {
		return map.entrySet().iterator();
	}

	/** a wrapper for Map.Entry sets */
	class EntrySetProxy implements Set {
		private final Set set;
		EntrySetProxy(Set set) {
			this.set=set;
		}
		public boolean add(Object entry) {
			//write(); -- doesn't
			return set.add(entry);
		}
		public boolean addAll(Collection entries) {
			//write(); -- doesn't
			return set.addAll(entries);
		}
		public void clear() {
			write();
			set.clear();
		}
		public boolean contains(Object entry) {
			return set.contains(entry);
		}
		public boolean containsAll(Collection entries) {
			return set.containsAll(entries);
		}
		public boolean isEmpty() {
			return set.isEmpty();
		}
		public Iterator iterator() {
			return new EntryIteratorProxy( set.iterator() );
		}
		public boolean remove(Object entry) {
			write();
			return set.remove(entry);
		}
		public boolean removeAll(Collection entries) {
			write();
			return set.removeAll(entries);
		}
		public boolean retainAll(Collection entries) {
			write();
			return set.retainAll(entries);
		}
		public int size() {
			return set.size();
		}
		// amazingly, these two will work because AbstractCollection
		// uses iterator() to fill the array
		public Object[] toArray() {
			return set.toArray();
		}
		public Object[] toArray(Object[] array) {
			return set.toArray(array);
		}
	}
	final class EntryIteratorProxy implements Iterator {
		private final Iterator iter;
		EntryIteratorProxy(Iterator iter) {
			this.iter=iter;
		}
		public boolean hasNext() {
			return iter.hasNext();
		}
		public Object next() {
			return new MapEntryProxy( (Map.Entry) iter.next() );
		}
		public void remove() {
			write();
			iter.remove();
		}
	}

	final class MapEntryProxy implements Map.Entry {
		private final Map.Entry me;
		MapEntryProxy( Map.Entry me ) {
			this.me = me;
		}
		public Object getKey() { return me.getKey(); }
		public Object getValue() { return me.getValue(); }
		public boolean equals(Object o) { return me.equals(o); }
		public int hashCode() { return me.hashCode(); }
		// finally, what it's all about...
		public Object setValue(Object value) {
			write();
			return me.setValue(value);
		}
	}

	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
	throws HibernateException {
		Serializable[] array = ( Serializable[] ) disassembled;
		int size = array.length;
		beforeInitialize( persister, size );
		for ( int i = 0; i < size; i+=2 ) {
			map.put(
					persister.getIndexType().assemble( array[i], getSession(), owner ),
					persister.getElementType().assemble( array[i+1], getSession(), owner )
				);
		}
	}

	public Serializable disassemble(CollectionPersister persister) throws HibernateException {

		Serializable[] result = new Serializable[ map.size() * 2 ];
		Iterator iter = map.entrySet().iterator();
		int i=0;
		while ( iter.hasNext() ) {
			Map.Entry e = (Map.Entry) iter.next();
			result[i++] = persister.getIndexType().disassemble( e.getKey(), getSession(), null );
			result[i++] = persister.getElementType().disassemble( e.getValue(), getSession(), null );
		}
		return result;

	}

	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) 
	throws HibernateException {
		List deletes = new ArrayList();
		Iterator iter = ( (Map) getSnapshot() ).entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry e = (Map.Entry) iter.next();
			Object key = e.getKey();
			if ( e.getValue()!=null && map.get(key)==null ) {
				deletes.add( indexIsFormula ? e.getValue() : key );
			}
		}
		return deletes.iterator();
	}

	public boolean needsInserting(Object entry, int i, Type elemType) 
	throws HibernateException {
		final Map sn = (Map) getSnapshot();
		Map.Entry e = (Map.Entry) entry;
		return e.getValue()!=null && sn.get( e.getKey() )==null;
	}

	public boolean needsUpdating(Object entry, int i, Type elemType) 
	throws HibernateException {
		final Map sn = (Map) getSnapshot();
		Map.Entry e = (Map.Entry) entry;
		Object snValue = sn.get( e.getKey() );
		return e.getValue()!=null &&
			snValue!=null &&
			elemType.isDirty( snValue, e.getValue(), getSession() );
	}


	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		return ( (Map.Entry) entry ).getKey();
	}

	public Object getElement(Object entry) {
		return ( (Map.Entry) entry ).getValue();
	}

	public Object getSnapshotElement(Object entry, int i) {
		final Map sn = (Map) getSnapshot();
		return sn.get( ( (Map.Entry) entry ).getKey() );
	}

	public boolean equals(Object other) {
		read();
		return map.equals(other);
	}

	public int hashCode() {
		read();
		return map.hashCode();
	}

	public boolean entryExists(Object entry, int i) {
		return ( (Map.Entry) entry ).getValue()!=null;
	}

	final class Clear implements DelayedOperation {
		public void operate() {
			map.clear();
		}
		public Object getAddedInstance() {
			return null;
		}
		public Object getOrphan() {
			throw new UnsupportedOperationException("queued clear cannot be used with orphan delete");
		}
	}

	final class Put implements DelayedOperation {
		private Object index;
		private Object value;
		private Object old;
		
		public Put(Object index, Object value, Object old) {
			this.index = index;
			this.value = value;
			this.old = old;
		}
		public void operate() {
			map.put(index, value);
		}
		public Object getAddedInstance() {
			return value;
		}
		public Object getOrphan() {
			return old;
		}
	}

	final class Remove implements DelayedOperation {
		private Object index;
		private Object old;
		
		public Remove(Object index, Object old) {
			this.index = index;
			this.old = old;
		}
		public void operate() {
			map.remove(index);
		}
		public Object getAddedInstance() {
			return null;
		}
		public Object getOrphan() {
			return old;
		}
	}
}
