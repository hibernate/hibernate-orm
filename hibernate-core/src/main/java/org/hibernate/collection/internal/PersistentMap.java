/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;


/**
 * A persistent wrapper for a <tt>java.util.Map</tt>. Underlying collection
 * is a <tt>HashMap</tt>.
 *
 * @see java.util.HashMap
 * @author Gavin King
 */
public class PersistentMap<K,V> extends AbstractPersistentCollection<V> implements Map<K,V> {
	protected Map<K,V> map;

	/**
	 * Empty constructor.
	 * <p/>
	 * Note: this form is not ever ever ever used by Hibernate; it is, however,
	 * needed for SOAP libraries and other such marshalling code.
	 */
	protected PersistentMap() {
		// intentionally empty
	}

	public PersistentMap(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,?,V> collectionDescriptor) {
		super( session, collectionDescriptor );
	}

	/**
	 * Instantiates a non-lazy map (the underlying map is constructed
	 * from the incoming map reference).
	 *
	 * @param session The session to which this map will belong.
	 * @param map The underlying map data.
	 */
	public PersistentMap(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,?,V> descriptor,
			Map<K,V> map) {
		this( session, descriptor );
		setMap( map );
		setDirectlyAccessible( true );
	}

	private void setMap(Map<K,V> map) {
		this.map = map;
		setInitialized();
	}

	public PersistentMap(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,?,V> descriptor,
			Object key) {
		super( session, descriptor, key );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public Serializable getSnapshot(PersistentCollectionDescriptor descriptor) throws HibernateException {
		final HashMap clonedMap = new HashMap( map.size() );
		for ( Object o : map.entrySet() ) {
			final Entry e = (Entry) o;
			final Object copy = descriptor.getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().deepCopy( e.getValue() );
			clonedMap.put( e.getKey(), copy );
		}
		return clonedMap;
	}

	@Override
	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final Map sn = (Map) snapshot;
		return getOrphans( sn.values(), map.values(), entityName, getSession() );
	}

	@Override
	public boolean equalsSnapshot(PersistentCollectionDescriptor collectionDescriptor) throws HibernateException {
		final Map snapshotMap = (Map) getSnapshot();
		if ( snapshotMap.size() != this.map.size() ) {
			return false;
		}

		for ( Object o : map.entrySet() ) {
			final Entry entry = (Entry) o;
			if ( collectionDescriptor.isDirty( entry.getValue(), snapshotMap.get( entry.getKey() ), getSession() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Map) snapshot ).isEmpty();
	}

	@Override
	public boolean isWrapper(Object collection) {
		return map==collection;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void beforeInitialize(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		this.map = (Map) getCollectionDescriptor().instantiateRaw( anticipatedSize );
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
	public V get(Object key) {
		final V result = readElementByIndex( key );
		return result == UNKNOWN
				? map.get( key )
				: result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public V put(K key, V value) {
		if ( isPutQueueEnabled() ) {
			final V old = readElementByIndex( key );
			if ( old != UNKNOWN ) {
				queueOperation( new Put( key, value, old ) );
				return old;
			}
		}
		initialize( true );
		final V old = map.put( key, value );
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
	@SuppressWarnings("unchecked")
	public V remove(Object key) {
		if ( isPutQueueEnabled() ) {
			final V old = readElementByIndex( key );
			if ( old != UNKNOWN ) {
				elementRemoved = true;
				queueOperation( new Remove( (K) key, old ) );
				return old;
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
	@SuppressWarnings("unchecked")
	public void putAll(Map<? extends K, ? extends V> puts) {
		if ( puts.size() > 0 ) {
			initialize( true );
			for ( Entry<? extends K, ? extends V> entry : puts.entrySet() ) {
				put( entry.getKey(), entry.getValue() );
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
	public Set keySet() {
		read();
		return new SetProxy( map.keySet() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection values() {
		read();
		return new SetProxy( map.values() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set entrySet() {
		read();
		return new EntrySetProxy( map.entrySet() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean empty() {
		return map.isEmpty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toString() {
		read();
		return map.toString();
	}

	private transient List<Object[]> loadingEntries;

	@Override
	@SuppressWarnings("unchecked")
	public Object readFrom(
			ResultSet rs,
			Object owner, PersistentCollectionDescriptor collectionDescriptor) {
		throw new NotYetImplementedFor6Exception(  );
//		final Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
//		if ( element != null ) {
//			final Object index = persister.readIndex( rs, descriptor.getSuffixedIndexAliases(), getSession() );
//			if ( loadingEntries == null ) {
//				loadingEntries = new ArrayList<>();
//			}
//			loadingEntries.add( new Object[] { index, element } );
//		}
//		return element;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean endRead() {
		if ( loadingEntries != null ) {
			for ( Object[] entry : loadingEntries ) {
				map.put( (K) entry[0], (V) entry[1] );
			}
			loadingEntries = null;
		}
		return super.endRead();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator entries(PersistentCollectionDescriptor descriptor) {
		return map.entrySet().iterator();
	}

	public void load(K key, V value) {
		assert isInitializing();
		map.put( key, value );
	}

	/**
	 * a wrapper for Map.Entry sets
	 */
	class EntrySetProxy implements Set {
		private final Set set;
		EntrySetProxy(Set set) {
			this.set=set;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean add(Object entry) {
			//write(); -- doesn't
			return set.add( entry );
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean addAll(Collection entries) {
			//write(); -- doesn't
			return set.addAll( entries );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void clear() {
			write();
			set.clear();
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean contains(Object entry) {
			return set.contains( entry );
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean containsAll(Collection entries) {
			return set.containsAll( entries );
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean isEmpty() {
			return set.isEmpty();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Iterator iterator() {
			return new EntryIteratorProxy( set.iterator() );
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean remove(Object entry) {
			write();
			return set.remove( entry );
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean removeAll(Collection entries) {
			write();
			return set.removeAll( entries );
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean retainAll(Collection entries) {
			write();
			return set.retainAll( entries );
		}

		@Override
		@SuppressWarnings("unchecked")
		public int size() {
			return set.size();
		}

		// amazingly, these two will work because AbstractCollection
		// uses iterator() to fill the array

		@Override
		@SuppressWarnings("unchecked")
		public Object[] toArray() {
			return set.toArray();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object[] toArray(Object[] array) {
			return set.toArray( array );
		}
	}

	final class EntryIteratorProxy implements Iterator {
		private final Iterator iter;
		EntryIteratorProxy(Iterator iter) {
			this.iter=iter;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object next() {
			return new MapEntryProxy( (Map.Entry) iter.next() );
		}

		@Override
		@SuppressWarnings("unchecked")
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

		@Override
		@SuppressWarnings("unchecked")
		public Object getKey() {
			return me.getKey();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object getValue() {
			return me.getValue();
		}

		@Override
		@SuppressWarnings({"unchecked", "EqualsWhichDoesntCheckParameterClass"})
		public boolean equals(Object o) {
			return me.equals( o );
		}

		@Override
		@SuppressWarnings("unchecked")
		public int hashCode() {
			return me.hashCode();
		}

		// finally, what it's all about...
		@Override
		@SuppressWarnings("unchecked")
		public Object setValue(Object value) {
			write();
			return me.setValue( value );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initializeFromCache(
			Serializable disassembled,
			Object owner,
			PersistentCollectionDescriptor collectionDescriptor)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;
		beforeInitialize( size, collectionDescriptor );
		for ( int i = 0; i < size; i+=2 ) {
			map.put(
					(K) getCollectionDescriptor().getIndexDescriptor()
							.getJavaTypeDescriptor()
							.getMutabilityPlan()
							.assemble( array[i] ),
					getCollectionDescriptor().getElementDescriptor()
							.getJavaTypeDescriptor()
							.getMutabilityPlan()
							.assemble( array[i+1] )
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Serializable disassemble(PersistentCollectionDescriptor collectionDescriptor) throws HibernateException {
		final Serializable[] result = new Serializable[ map.size() * 2 ];
		final Iterator itr = map.entrySet().iterator();
		int i=0;
		while ( itr.hasNext() ) {
			final Map.Entry e = (Map.Entry) itr.next();
			result[i++] = collectionDescriptor.getIndexDescriptor()
					.getJavaTypeDescriptor()
					.getMutabilityPlan()
					.disassemble( e.getKey() );
			result[i++] = collectionDescriptor.getElementDescriptor()
					.getJavaTypeDescriptor()
					.getMutabilityPlan()
					.disassemble( e.getValue() );
		}
		return result;

	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator getDeletes(PersistentCollectionDescriptor descriptor, boolean indexIsFormula) throws HibernateException {
		final List deletes = new ArrayList();
		for ( Object o : ((Map) getSnapshot()).entrySet() ) {
			final Entry e = (Entry) o;
			final Object key = e.getKey();
			if ( e.getValue() != null && map.get( key ) == null ) {
				deletes.add( indexIsFormula ? e.getValue() : key );
			}
		}
		return deletes.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean needsInserting(Object entry, int i) throws HibernateException {
		final Map sn = (Map) getSnapshot();
		final Map.Entry e = (Map.Entry) entry;
		return e.getValue() != null && sn.get( e.getKey() ) == null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean needsUpdating(Object entry, int i) throws HibernateException {
		final Map sn = (Map) getSnapshot();
		final Map.Entry e = (Map.Entry) entry;
		final Object snValue = sn.get( e.getKey() );
		return e.getValue() != null
				&& snValue != null
				&& getCollectionDescriptor().isDirty( snValue, e.getValue(), getSession() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getIndex(
			Object entry,
			int assumedIndex,
			PersistentCollectionDescriptor collectionDescriptor) {
		return ( (Map.Entry) entry ).getKey();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getElement(
			Object entry,
			PersistentCollectionDescriptor collectionDescriptor) {
		return ( (Map.Entry) entry ).getValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public V getSnapshotElement(Object entry, int index) {
		final Map<K,V> sn = (Map) getSnapshot();
		return sn.get( ( (Map.Entry<K,V>) entry ).getKey() );
	}

	@Override
	@SuppressWarnings({"unchecked", "EqualsWhichDoesntCheckParameterClass"})
	public boolean equals(Object other) {
		read();
		return map.equals( other );
	}

	@Override
	@SuppressWarnings("unchecked")
	public int hashCode() {
		read();
		return map.hashCode();
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean entryExists(Object entry, int i) {
		return ( (Map.Entry) entry ).getValue() != null;
	}

	final class Clear implements DelayedOperation {
		@Override
		public void operate() {
			map.clear();
		}

		@Override
		public Object getAddedInstance() {
			return null;
		}

		@Override
		public Object getOrphan() {
			throw new UnsupportedOperationException( "queued clear cannot be used with orphan delete" );
		}
	}

	abstract class AbstractMapValueDelayedOperation extends AbstractValueDelayedOperation {
		private K index;

		protected AbstractMapValueDelayedOperation(K index, V addedValue, Object orphan) {
			super( addedValue, orphan );
			this.index = index;
		}

		protected final K getIndex() {
			return index;
		}
	}

	final class Put extends AbstractMapValueDelayedOperation {

		public Put(K index, V addedValue, Object orphan) {
			super( index, addedValue, orphan );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operate() {
			map.put( getIndex(), getAddedInstance() );
		}
	}

	final class Remove extends AbstractMapValueDelayedOperation {

		public Remove(K index, Object orphan) {
			super( index, null, orphan );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operate() {
			map.remove( getIndex() );
		}
	}
}
