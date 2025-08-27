/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;


/**
 * A persistent wrapper for a {@link java.util.Map}. Underlying collection
 * is a {@code HashMap}.
 *
 * @apiNote Incubating in terms of making this non-internal.
 *          These contracts will be getting cleaned up in following
 *          releases.
 *
 * @author Gavin King
 */
@Incubating
public class PersistentMap<K,E> extends AbstractPersistentCollection<E> implements Map<K,E> {

	protected Map<K,E> map;

	/**
	 * Empty constructor.
	 * <p>
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
	public PersistentMap(SharedSessionContractImplementor session) {
		super( session );
	}

	/**
	 * Instantiates a non-lazy map (the underlying map is constructed
	 * from the incoming map reference).
	 *
	 * @param session The session to which this map will belong.
	 * @param map The underlying map data.
	 */
	public PersistentMap(SharedSessionContractImplementor session, Map<K,E> map) {
		super( session );
		this.map = map;
		setInitialized();
		setDirectlyAccessible( true );
	}

	@Override
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		final HashMap<K,E> clonedMap = CollectionHelper.mapOfSize( map.size() );
		for ( Entry<K,E> e : map.entrySet() ) {
			final E copy = (E) persister.getElementType().deepCopy( e.getValue(), persister.getFactory() );
			clonedMap.put( e.getKey(), copy );
		}
		return clonedMap;
	}

	@Override
	public Collection<E> getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final Map<K,E> sn = (Map<K,E>) snapshot;
		return getOrphans( sn.values(), map.values(), entityName, getSession() );
	}

	@Override
	public void initializeEmptyCollection(CollectionPersister persister) {
		assert map == null;
		//noinspection unchecked
		map = (Map<K,E>) persister.getCollectionSemantics().instantiateRaw( 0, persister );
		endRead();
	}

	@Override
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final Map<?,?> snapshotMap = (Map<?,?>) getSnapshot();
		if ( snapshotMap.size() != this.map.size() ) {
			return false;
		}

		for ( Entry<?,?> entry : map.entrySet() ) {
			if ( elementType.isDirty( entry.getValue(), snapshotMap.get( entry.getKey() ), getSession() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Map<?,?>) snapshot ).isEmpty();
	}

	@Override
	public boolean isWrapper(Object collection) {
		return map==collection;
	}

	@Override
	public int size() {
		return readSize() ? getCachedSize() : map.size();
	}

	@Override
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		final Boolean exists = readIndexExistence( key );
		return exists == null ? map.containsKey( key ) : exists;
	}

	@Override
	public boolean containsValue(Object value) {
		final Boolean exists = readElementExistence( value );
		return exists == null
				? map.containsValue( value )
				: exists;
	}

	@Override
	public E get(Object key) {
		final Object result = readElementByIndex( key );
		return result == UNKNOWN
				? map.get( key )
				: (E) result;
	}

	@Override
	public E put(K key, E value) {
		if ( isPutQueueEnabled() ) {
			final Object old = readElementByIndex( key );
			if ( old != UNKNOWN ) {
				queueOperation( new Put( key, value, (E) old ) );
				return (E) old;
			}
		}
		initialize( true );
		final E old = map.put( key, value );
		// would be better to use the element-type to determine
		// whether the old and the new are equal here; the problem being
		// we do not necessarily have access to the element type in all
		// cases
		if ( value != old ) {
			dirty();
		}
		return old;
	}

	@Override
	public E remove(Object key) {
		if ( isPutQueueEnabled() ) {
			final Object old = readElementByIndex( key );
			if ( old != UNKNOWN ) {
				elementRemoved = true;
				queueOperation( new Remove( (K) key, (E) old ) );
				return (E) old;
			}
		}
		// TODO : safe to interpret "map.remove(key) == null" as non-dirty?
		initialize( true );
		if ( map.containsKey( key ) ) {
			elementRemoved = true;
			dirty();
		}
		return map.remove( key );
	}

	@Override
	public void putAll(Map<? extends K,? extends E> puts) {
		if ( puts.size() > 0 ) {
			initialize( true );
			for ( Entry<? extends K,? extends E> entry : puts.entrySet() ) {
				put( entry.getKey(), entry.getValue() );
			}
		}
	}

	@Override
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

	@Override
	public Set<K> keySet() {
		read();
		return new SetProxy<>( map.keySet() );
	}

	@Override
	public Collection<E> values() {
		read();
		return new SetProxy<>( map.values() );
	}

	@Override
	public Set<Entry<K,E>> entrySet() {
		read();
		return new EntrySetProxy( map.entrySet() );
	}

	@Override
	public boolean empty() {
		return map.isEmpty();
	}

	@Override
	public String toString() {
		read();
		return map.toString();
	}

	@Override
	public Iterator<Entry<K,E>> entries(CollectionPersister persister) {
		return map.entrySet().iterator();
	}

	public void injectLoadedState(PluralAttributeMapping attributeMapping, List<?> loadingState) {
		assert isInitializing();
		assert map == null;

		final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();
		this.map = (Map<K,E>) collectionDescriptor.getCollectionSemantics().instantiateRaw( loadingState.size(), collectionDescriptor );

		for ( int i = 0; i < loadingState.size(); i++ ) {
			final Object[] keyVal = (Object[]) loadingState.get( i );
			map.put( (K) keyVal[0], (E) keyVal[1] );
		}
	}

	@Override
	public void initializeFromCache(CollectionPersister persister, Object disassembled, Object owner)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;

		this.map = (Map<K,E>) persister.getCollectionSemantics().instantiateRaw( size, persister );

		for ( int i = 0; i < size; i+=2 ) {
			map.put(
					(K) persister.getIndexType().assemble( array[i], getSession(), owner ),
					(E) persister.getElementType().assemble( array[i+1], getSession(), owner )
			);
		}
	}

	@Override
	public Object disassemble(CollectionPersister persister) throws HibernateException {
		final Serializable[] result = new Serializable[ map.size() * 2 ];
		final Iterator<Entry<K,E>> itr = map.entrySet().iterator();
		int i=0;
		while ( itr.hasNext() ) {
			final Entry<K,E> e = itr.next();
			result[i++] = persister.getIndexType().disassemble( e.getKey(), getSession(), null );
			result[i++] = persister.getElementType().disassemble( e.getValue(), getSession(), null );
		}
		return result;

	}

	/**
	 * a wrapper for Map.Entry sets
	 */
	class EntrySetProxy implements Set<Entry<K,E>> {
		private final Set<Entry<K,E>> set;
		EntrySetProxy(Set<Entry<K,E>> set) {
			this.set=set;
		}

		@Override
		public boolean add(Entry<K,E> entry) {
			//write(); -- doesn't
			return set.add( entry );
		}

		@Override
		public boolean addAll(Collection<? extends Entry<K,E>> entries) {
			//write(); -- doesn't
			return set.addAll( entries );
		}

		@Override
		public void clear() {
			write();
			set.clear();
		}

		@Override
		public boolean contains(Object entry) {
			return set.contains( entry );
		}

		@Override
		public boolean containsAll(Collection<?> entries) {
			return set.containsAll( entries );
		}

		@Override
		public boolean isEmpty() {
			return set.isEmpty();
		}

		@Override
		public Iterator<Entry<K,E>> iterator() {
			return new EntryIteratorProxy( set.iterator() );
		}

		@Override
		public boolean remove(Object entry) {
			write();
			return set.remove( entry );
		}

		@Override
		public boolean removeAll(Collection<?> entries) {
			write();
			return set.removeAll( entries );
		}

		@Override
		public boolean retainAll(Collection<?> entries) {
			write();
			return set.retainAll( entries );
		}

		@Override
		public int size() {
			return set.size();
		}

		// amazingly, these two will work because AbstractCollection
		// uses iterator() to fill the array

		@Override
		public Object[] toArray() {
			return set.toArray();
		}

		@Override
		public <A> A[] toArray(A[] array) {
			return set.toArray( array );
		}
	}

	final class EntryIteratorProxy implements Iterator<Entry<K,E>> {
		private final Iterator<Entry<K,E>> iter;
		EntryIteratorProxy(Iterator<Entry<K,E>> iter) {
			this.iter=iter;
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public Entry<K,E> next() {
			return new MapEntryProxy( iter.next() );
		}

		@Override
		public void remove() {
			write();
			iter.remove();
		}
	}

	final class MapEntryProxy implements Entry<K,E> {
		private final Entry<K,E> me;
		MapEntryProxy(Entry<K,E> me) {
			this.me = me;
		}

		@Override
		public K getKey() {
			return me.getKey();
		}

		@Override
		public E getValue() {
			return me.getValue();
		}

		@Override
		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		public boolean equals(Object o) {
			return me.equals( o );
		}

		@Override
		public int hashCode() {
			return me.hashCode();
		}

		// finally, what it's all about...
		@Override
		public E setValue(E value) {
			write();
			return me.setValue( value );
		}
	}

	@Override
	public Iterator<?> getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final List<Object> deletes = new ArrayList<>();
		for ( Entry<?,?> e : ((Map<?,?>) getSnapshot()).entrySet() ) {
			final Object key = e.getKey();
			if ( e.getValue() != null && map.get( key ) == null ) {
				deletes.add( indexIsFormula ? e.getValue() : key );
			}
		}
		return deletes.iterator();
	}

	@Override
	public boolean hasDeletes(CollectionPersister persister) {
		for ( Entry<?,?> e : ((Map<?,?>) getSnapshot()).entrySet() ) {
			if ( e.getValue() != null && map.get( e.getKey() ) == null ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		final Map<?,?> sn = (Map<?,?>) getSnapshot();
		final Entry<?,?> e = (Entry<?,?>) entry;
		return e.getValue() != null && sn.get( e.getKey() ) == null;
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {
		final Map<?,?> sn = (Map<?,?>) getSnapshot();
		final Entry<?,?> e = (Entry<?,?>) entry;
		final Object snValue = sn.get( e.getKey() );
		return e.getValue() != null
				&& snValue != null
				&& elemType.isDirty( snValue, e.getValue(), getSession() );
	}

	@Override
	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		return ( (Entry<?,?>) entry ).getKey();
	}

	@Override
	public Object getElement(Object entry) {
		return ( (Entry<?,?>) entry ).getValue();
	}

	@Override
	public Object getSnapshotElement(Object entry, int i) {
		final Map<?,?> sn = (Map<?,?>) getSnapshot();
		return sn.get( ( (Entry<?,?>) entry ).getKey() );
	}

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object other) {
		read();
		return map.equals( other );
	}

	@Override
	public int hashCode() {
		read();
		return map.hashCode();
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return ( (Entry<?,?>) entry ).getValue() != null;
	}

	final class Clear implements DelayedOperation<E> {
		@Override
		public void operate() {
			map.clear();
		}

		@Override
		public E getAddedInstance() {
			return null;
		}

		@Override
		public E getOrphan() {
			throw new UnsupportedOperationException( "queued clear cannot be used with orphan delete" );
		}
	}

	abstract class AbstractMapValueDelayedOperation extends AbstractValueDelayedOperation {
		private final K index;

		protected AbstractMapValueDelayedOperation(K index, E addedValue, E orphan) {
			super( addedValue, orphan );
			this.index = index;
		}

		protected final K getIndex() {
			return index;
		}
	}

	final class Put extends AbstractMapValueDelayedOperation {

		public Put(K index, E addedValue, E orphan) {
			super( index, addedValue, orphan );
		}

		@Override
		public void operate() {
			map.put( getIndex(), getAddedInstance() );
		}
	}

	final class Remove extends AbstractMapValueDelayedOperation {

		public Remove(K index, E orphan) {
			super( index, null, orphan );
		}

		@Override
		public void operate() {
			map.remove( getIndex() );
		}
	}
}
