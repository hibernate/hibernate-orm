package org.hibernate.internal.util.collections;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 *
 * @author Strong Liu <stliu@hibernate.org>
 */
public class WeakValueHashMap<K, V>
		extends AbstractMap<K, V>
		implements Map<K, V> {

	private static final int DEFAULT_INITIAL_CAPACITY = 16;
	private static final int MAXIMUM_CAPACITY = 1 << 30;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private Entry[] table;
	private int size;
	private int threshold;
	private final float loadFactor;
	private final ReferenceQueue<V> queue = new ReferenceQueue<V>();
	private volatile int modCount;

	/**
	 * Constructs a new, empty <tt>WeakValueHashMap</tt> with the given initial
	 * capacity and the given load factor.
	 *
	 * @param initialCapacity The initial capacity of the <tt>WeakValueHashMap</tt>
	 * @param loadFactor The load factor of the <tt>WeakValueHashMap</tt>
	 *
	 * @throws IllegalArgumentException if the initial capacity is negative,
	 * or if the load factor is nonpositive.
	 */
	public WeakValueHashMap(int initialCapacity, float loadFactor) {
		if ( initialCapacity < 0 ) {
			throw new IllegalArgumentException(
					"Illegal Initial Capacity: " +
							initialCapacity
			);
		}
		if ( initialCapacity > MAXIMUM_CAPACITY ) {
			initialCapacity = MAXIMUM_CAPACITY;
		}

		if ( loadFactor <= 0 || Float.isNaN( loadFactor ) ) {
			throw new IllegalArgumentException(
					"Illegal Load factor: " +
							loadFactor
			);
		}
		int capacity = 1;
		while ( capacity < initialCapacity ) {
			capacity <<= 1;
		}
		this.table = new Entry[capacity];
		this.loadFactor = loadFactor;
		this.threshold = (int) ( capacity * loadFactor );
	}

	/**
	 * Constructs a new, empty <tt>WeakValueHashMap</tt> with the given initial
	 * capacity and the default load factor (0.75).
	 *
	 * @param initialCapacity The initial capacity of the <tt>WeakValueHashMap</tt>
	 *
	 * @throws IllegalArgumentException if the initial capacity is negative
	 */
	public WeakValueHashMap(int initialCapacity) {
		this( initialCapacity, DEFAULT_LOAD_FACTOR );
	}

	/**
	 * Constructs a new, empty <tt>WeakValueHashMap</tt> with the default initial
	 * capacity (16) and load factor (0.75).
	 */
	public WeakValueHashMap() {
		this( DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR );
	}

	/**
	 * Constructs a new <tt>WeakValueHashMap</tt> with the same mappings as the
	 * specified map.  The <tt>WeakValueHashMap</tt> is created with the default
	 * load factor (0.75) and an initial capacity sufficient to hold the
	 * mappings in the specified map.
	 *
	 * @param m the map whose mappings are to be placed in this map
	 *
	 * @throws NullPointerException if the specified map is null
	 * @since 1.3
	 */
	public WeakValueHashMap(Map<? extends K, ? extends V> m) {
		this(
				Math.max( (int) ( m.size() / DEFAULT_LOAD_FACTOR ) + 1, 16 ),
				DEFAULT_LOAD_FACTOR
		);
		putAll( m );
	}

	private static boolean eq(Object x, Object y) {
		return x == y || x.equals( y );
	}

	private static int indexFor(int h, int length) {
		return h & ( length - 1 );
	}

	/**
	 * Expunges stale entries from the table.
	 */
	private void expungeStaleEntries() {
		Entry<K, V> e;
		while ( ( e = (Entry<K, V>) queue.poll() ) != null ) {
			int h = e.hash;
			int i = indexFor( h, table.length );

			Entry<K, V> prev = table[i];
			Entry<K, V> p = prev;
			while ( p != null ) {
				Entry<K, V> next = p.next;
				if ( p == e ) {
					if ( prev == e ) {
						table[i] = next;
					}
					else {
						prev.next = next;
					}
					e.next = null;
					e.key = null;
					size--;
					break;
				}
				prev = p;
				p = next;
			}
		}
	}

	/**
	 * Returns the table after first expunging stale entries.
	 */
	private Entry[] getTable() {
		expungeStaleEntries();
		return table;
	}

	@Override
	public int size() {
		if ( size == 0 ) {
			return 0;
		}
		expungeStaleEntries();
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public V get(Object key) {
		if ( key == null ) {
			return null;
		}
		Object k = key;
		int h = hash( k.hashCode() );
		Entry[] tab = getTable();
		int index = indexFor( h, tab.length );
		Entry<K, V> e = tab[index];
		while ( e != null ) {
			if ( e.hash == h && eq( k, e.key ) ) {
				return e.get();
			}
			e = e.next;
		}
		return null;
	}

	private static int hash(int h) {
		h ^= ( h >>> 20 ) ^ ( h >>> 12 );
		return h ^ ( h >>> 7 ) ^ ( h >>> 4 );
	}

	@Override
	public boolean containsKey(Object key) {
		return key == null ? false : getEntry( key ) != null;
	}

	private Entry<K, V> getEntry(Object key) {
		if ( key == null ) {
			return null;
		}
		Object k = key;
		int h = hash( k.hashCode() );
		Entry[] tab = getTable();
		int index = indexFor( h, tab.length );
		Entry<K, V> e = tab[index];
		while ( e != null && !( e.hash == h && eq( k, e.get() ) ) ) {
			e = e.next;
		}
		return e;
	}

	@Override
	public V put(K key, V value) {
		if ( key == null || value == null ) {
			return null;  // this map impl doesn't allow null key / value
		}
		K k = key;
		int h = hash( k.hashCode() );
		Entry[] tab = getTable();
		int i = indexFor( h, tab.length );

		for ( Entry<K, V> e = tab[i]; e != null; e = e.next ) {
			if ( h == e.hash && eq( k, e.key ) ) {
				V oldValue = e.get();
				if ( value != oldValue ) {
					Entry next = e.next;
					tab[i] = new Entry<K, V>( key, value, queue, h, next );
				}
				return oldValue;
			}
		}

		modCount++;
		Entry<K, V> e = tab[i];
		tab[i] = new Entry<K, V>( k, value, queue, h, e );
		if ( ++size >= threshold ) {
			resize( tab.length * 2 );
		}
		return null;
	}


	private void resize(int newCapacity) {
		Entry[] oldTable = getTable();
		int oldCapacity = oldTable.length;
		if ( oldCapacity == MAXIMUM_CAPACITY ) {
			threshold = Integer.MAX_VALUE;
			return;
		}

		Entry[] newTable = new Entry[newCapacity];
		transfer( oldTable, newTable );
		table = newTable;

		/*
				 * If ignoring null elements and processing ref queue caused massive
				 * shrinkage, then restore old table.  This should be rare, but avoids
				 * unbounded expansion of garbage-filled tables.
				 */
		if ( size >= threshold / 2 ) {
			threshold = (int) ( newCapacity * loadFactor );
		}
		else {
			expungeStaleEntries();
			transfer( newTable, oldTable );
			table = oldTable;
		}
	}

	/**
	 * Transfers all entries from src to dest tables
	 */
	private void transfer(Entry[] src, Entry[] dest) {
		for ( int j = 0; j < src.length; ++j ) {
			Entry<K, V> e = src[j];
			src[j] = null;
			while ( e != null ) {
				Entry<K, V> next = e.next;
				Object key = e.key;
				if ( key == null ) {
					e.next = null;
					size--;
				}
				else {
					int i = indexFor( e.hash, dest.length );
					e.next = dest[i];
					dest[i] = e;
				}
				e = next;
			}
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		int numKeysToBeAdded = m.size();
		if ( numKeysToBeAdded == 0 ) {
			return;
		}
		if ( numKeysToBeAdded > threshold ) {
			int targetCapacity = (int) ( numKeysToBeAdded / loadFactor + 1 );
			if ( targetCapacity > MAXIMUM_CAPACITY ) {
				targetCapacity = MAXIMUM_CAPACITY;
			}
			int newCapacity = table.length;
			while ( newCapacity < targetCapacity ) {
				newCapacity <<= 1;
			}
			if ( newCapacity > table.length ) {
				resize( newCapacity );
			}
		}

		for ( Map.Entry<? extends K, ? extends V> e : m.entrySet() ) {
			put( e.getKey(), e.getValue() );
		}
	}

	@Override
	public V remove(Object key) {
		if ( key == null ) {
			return null;
		}
		int h = hash( key.hashCode() );
		Entry[] tab = getTable();
		int i = indexFor( h, tab.length );
		Entry<K, V> prev = tab[i];
		Entry<K, V> e = prev;

		while ( e != null ) {
			Entry<K, V> next = e.next;
			if ( h == e.hash && eq( key, e.key ) ) {
				modCount++;
				size--;
				if ( prev == e ) {
					tab[i] = next;
				}
				else {
					prev.next = next;
				}
				return e.get();
			}
			prev = e;
			e = next;
		}

		return null;
	}


	private Entry<K, V> removeMapping(Object o) {
		if ( !( o instanceof Map.Entry ) ) {
			return null;
		}
		Entry[] tab = getTable();
		Map.Entry entry = (Map.Entry) o;
		Object k = entry.getKey();
		int h = hash( k.hashCode() );
		int i = indexFor( h, tab.length );
		Entry<K, V> prev = tab[i];
		Entry<K, V> e = prev;

		while ( e != null ) {
			Entry<K, V> next = e.next;
			if ( h == e.hash && e.equals( entry ) ) {
				modCount++;
				size--;
				if ( prev == e ) {
					tab[i] = next;
				}
				else {
					prev.next = next;
				}
				return e;
			}
			prev = e;
			e = next;
		}

		return null;
	}

	@Override
	public void clear() {
		while ( queue.poll() != null ) {
			;
		}

		modCount++;
		Entry[] tab = table;
		for ( int i = 0; i < tab.length; ++i ) {
			tab[i] = null;
		}
		size = 0;

		// Allocation of array may have caused GC, which may have caused
		// additional entries to go stale.  Removing these entries from the
		// reference queue will make them eligible for reclamation.
		while ( queue.poll() != null ) {
			;
		}
	}

	@Override
	public boolean containsValue(Object value) {
		if ( value == null ) {
			return false;
		}

		Entry[] tab = getTable();
		for ( int i = tab.length; i-- > 0; ) {
			for ( Entry e = tab[i]; e != null; e = e.next ) {
				if ( value.equals( e.get() ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static class Entry<K, V> extends WeakReference<V> implements Map.Entry<K, V> {
		private K key;
		private final int hash;
		private Entry<K, V> next;

		Entry(K key, V value,
			  ReferenceQueue<V> queue,
			  int hash, Entry<K, V> next) {
			super( value, queue );
			this.key = key;
			this.hash = hash;
			this.next = next;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return get();
		}

		@Override
		public V setValue(V newValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean equals(Object o) {
			if ( !( o instanceof Map.Entry ) ) {
				return false;
			}
			Map.Entry e = (Map.Entry) o;
			Object k1 = getKey();
			Object k2 = e.getKey();
			if ( k1 == k2 || ( k1 != null && k1.equals( k2 ) ) ) {
				Object v1 = getValue();
				Object v2 = e.getValue();
				if ( v1 == v2 || ( v1 != null && v1.equals( v2 ) ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			Object k = getKey();
			Object v = getValue();
			return ( ( k == null ? 0 : k.hashCode() ) ^
					( v == null ? 0 : v.hashCode() ) );
		}

		@Override
		public String toString() {
			return getKey() + "=" + getValue();
		}
	}

	private abstract class HashIterator<T> implements Iterator<T> {
		int index;
		Entry<K, V> entry = null;
		Entry<K, V> lastReturned = null;
		int expectedModCount = modCount;
		Object nextKey = null;
		Object currentKey = null;

		HashIterator() {
			index = ( size() != 0 ? table.length : 0 );
		}

		@Override
		public boolean hasNext() {
			Entry[] t = table;

			while ( nextKey == null ) {
				Entry<K, V> e = entry;
				int i = index;
				while ( e == null && i > 0 ) {
					e = t[--i];
				}
				entry = e;
				index = i;
				if ( e == null ) {
					currentKey = null;
					return false;
				}
				nextKey = e.get(); // hold on to key in strong ref
				if ( nextKey == null ) {
					entry = entry.next;
				}
			}
			return true;
		}

		protected Entry<K, V> nextEntry() {
			if ( modCount != expectedModCount ) {
				throw new ConcurrentModificationException();
			}
			if ( nextKey == null && !hasNext() ) {
				throw new NoSuchElementException();
			}

			lastReturned = entry;
			entry = entry.next;
			currentKey = nextKey;
			nextKey = null;
			return lastReturned;
		}

		@Override
		public void remove() {
			if ( lastReturned == null ) {
				throw new IllegalStateException();
			}
			if ( modCount != expectedModCount ) {
				throw new ConcurrentModificationException();
			}

			WeakValueHashMap.this.remove( currentKey );
			expectedModCount = modCount;
			lastReturned = null;
			currentKey = null;
		}

	}

	private class ValueIterator extends HashIterator<V> {
		@Override
		public V next() {
			return nextEntry().get();
		}
	}

	private class KeyIterator extends HashIterator<K> {
		@Override
		public K next() {
			return nextEntry().getKey();
		}
	}

	private class EntryIterator extends HashIterator<Map.Entry<K, V>> {
		@Override
		public Map.Entry<K, V> next() {
			return nextEntry();
		}
	}

	private transient Set<Map.Entry<K, V>> entrySet = null;
	private transient volatile Set<K> keySet = null;
	private transient volatile Collection<V> values = null;

	@Override
	public Set<K> keySet() {
		if ( keySet == null ) {
			keySet = new KeySet();
		}
		return keySet;
	}

	private class KeySet extends AbstractSet<K> {
		@Override
		public Iterator<K> iterator() {
			return new KeyIterator();
		}

		@Override
		public int size() {
			return WeakValueHashMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return containsKey( o );
		}

		@Override
		public boolean remove(Object o) {
			if ( containsKey( o ) ) {
				WeakValueHashMap.this.remove( o );
				return true;
			}
			return false;
		}

		@Override
		public void clear() {
			WeakValueHashMap.this.clear();
		}
	}

	@Override
	public Collection<V> values() {
		if ( values == null ) {
			values = new Values();
		}
		return values;
	}

	private class Values extends AbstractCollection<V> {
		@Override
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public int size() {
			return WeakValueHashMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return containsValue( o );
		}

		@Override
		public void clear() {
			WeakValueHashMap.this.clear();
		}
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		if ( entrySet == null ) {
			entrySet = new EntrySet();
		}
		return entrySet;
	}

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public boolean contains(Object o) {
			if ( !( o instanceof Map.Entry ) ) {
				return false;
			}
			Map.Entry e = (Map.Entry) o;
			Object k = e.getKey();
			Entry candidate = getEntry( k );
			return candidate != null && candidate.equals( e );
		}

		@Override
		public boolean remove(Object o) {
			return removeMapping( o ) != null;
		}

		@Override
		public int size() {
			return WeakValueHashMap.this.size();
		}

		@Override
		public void clear() {
			WeakValueHashMap.this.clear();
		}

		private List<Map.Entry<K, V>> deepCopy() {
			List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>( size() );
			for ( Map.Entry<K, V> e : this ) {
				list.add( new AbstractMap.SimpleEntry<K, V>( e ) );
			}
			return list;
		}

		@Override
		public Object[] toArray() {
			return deepCopy().toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return deepCopy().toArray( a );
		}
	}
}
