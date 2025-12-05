/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;


/**
 * Utility {@link Map} implementation that uses identity (==) for key comparison and preserves insertion order
 */
public class LinkedIdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
	private static final int DEFAULT_INITIAL_CAPACITY = 16; // must be power of two

	static final class Node<K, V> implements Map.Entry<K, V> {
		final K key;
		V value;
		Node<K, V> next;
		Node<K, V> before;
		Node<K, V> after;

		Node(K key, V value, Node<K, V> next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V newValue) {
			final V old = value;
			value = newValue;
			return old;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Node<?, ?> node && key == node.key && Objects.equals( value, node.value );
		}

		@Override
		public int hashCode() {
			int result = System.identityHashCode( key );
			result = 31 * result + Objects.hashCode( value );
			return result;
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}
	}

	private Node<K, V>[] table;
	private int size;

	private Node<K, V> head;
	private Node<K, V> tail;

	private transient Set<Map.Entry<K, V>> entrySet;

	public LinkedIdentityHashMap() {
		this( DEFAULT_INITIAL_CAPACITY );
	}

	public LinkedIdentityHashMap(int initialCapacity) {
		if ( initialCapacity < 0 ) {
			throw new IllegalArgumentException( "Illegal initial capacity: " + initialCapacity );
		}
		int cap = 1;
		while ( cap < initialCapacity ) {
			cap <<= 1;
		}
		//noinspection unchecked
		table = (Node<K, V>[]) new Node[cap];
	}

	private static int indexFor(int hash, int length) {
		return hash & (length - 1);
	}

	@Override
	public V get(Object key) {
		final Node<K, V> e = getNode( key );
		return e != null ? e.value : null;
	}

	private Node<K, V> getNode(Object key) {
		final int hash = System.identityHashCode( key );
		final int idx = indexFor( hash, table.length );
		for ( Node<K, V> e = table[idx]; e != null; e = e.next ) {
			if ( e.key == key ) {
				return e;
			}
		}
		return null;
	}

	@Override
	public boolean containsKey(Object key) {
		return getNode( key ) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		for ( Node<K, V> e = head; e != null; e = e.after ) {
			if ( Objects.equals( e.value, value ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public V put(K key, V value) {
		final int hash = System.identityHashCode( key );
		final int idx = indexFor( hash, table.length );
		for ( Node<K, V> e = table[idx]; e != null; e = e.next ) {
			if ( e.key == key ) {
				final V old = e.value;
				e.value = value;
				return old;
			}
		}
		// not found -> insert
		final Node<K, V> newNode = new Node<>( key, value, table[idx] );
		table[idx] = newNode;
		linkLast( newNode );
		size++;
		if ( size == table.length ) {
			resize();
		}
		return null;
	}

	private void linkLast(Node<K, V> node) {
		if ( tail == null ) {
			head = tail = node;
		}
		else {
			tail.after = node;
			node.before = tail;
			tail = node;
		}
	}

	@Override
	public V remove(Object key) {
		final int hash = System.identityHashCode( key );
		final int idx = indexFor( hash, table.length );
		Node<K, V> prev = null;
		for ( Node<K, V> e = table[idx]; e != null; prev = e, e = e.next ) {
			if ( e.key == key ) {
				// remove from bucket chain
				if ( prev == null ) {
					table[idx] = e.next;
				}
				else {
					prev.next = e.next;
				}
				// unlink from insertion-order list
				final Node<K, V> b = e.before;
				final Node<K, V> a = e.after;
				if ( b == null ) {
					head = a;
				}
				else {
					b.after = a;
				}
				if ( a == null ) {
					tail = b;
				}
				else {
					a.before = b;
				}
				size--;
				return e.value;
			}
		}
		return null;
	}

	@Override
	public void clear() {
		Arrays.fill( table, null );
		head = tail = null;
		size = 0;
	}

	@Override
	public int size() {
		return size;
	}

	private void resize() {
		final int oldCap = table.length;
		final int newCap = oldCap << 1;
		//noinspection unchecked
		final Node<K, V>[] newTable = (Node<K, V>[]) new Node[newCap];
		for ( int i = 0; i < oldCap; i++ ) {
			Node<K, V> e = table[i];
			while ( e != null ) {
				final Node<K, V> next = e.next;
				final int idx = indexFor( System.identityHashCode( e.key ), newCap );
				e.next = newTable[idx];
				newTable[idx] = e;
				e = next;
			}
		}
		table = newTable;
	}

	final class EntryIterator implements Iterator<Map.Entry<K, V>> {
		private Node<K, V> next = head;
		private Node<K, V> current = null;

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public Node<K, V> next() {
			Node<K, V> e = next;
			if ( e == null ) {
				throw new NoSuchElementException();
			}
			current = e;
			next = e.after;
			return e;
		}

		@Override
		public void remove() {
			Node<K, V> e = current;
			if ( e == null ) {
				throw new IllegalStateException();
			}
			LinkedIdentityHashMap.this.remove( e.key );
			current = null;
		}
	}

	final class EntrySet extends AbstractSet<Entry<K, V>> {
		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public int size() {
			return LinkedIdentityHashMap.this.size;
		}

		@Override
		public void clear() {
			LinkedIdentityHashMap.this.clear();
		}

		@Override
		public boolean contains(Object o) {
			if ( o instanceof Entry<?, ?> e ) {
				final Node<K, V> n = getNode( e.getKey() );
				return n != null && Objects.equals( n.value, e.getValue() );
			}
			return false;
		}

		@Override
		public boolean remove(Object o) {
			return o instanceof Entry<?, ?> e && LinkedIdentityHashMap.this.remove( e.getKey() ) != null;
		}
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> es;
		return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
	}
}
