/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Since Service lookup is a very hot operation and essentially it's a read only
 * data structure, to achieve threadsafety we can use immutability.
 * For our use case we just need reference equality, and the expectation is that a limited
 * number of elements will be contained in this custom collection (<32).
 * So the following structure is functionally equivalent to an Identity based ConcurrentMap,
 * but heavily tuned for reads, at cost of structural reorganization at writes.
 * The implementation is a binary tree basing the comparison order on the identityHashCode
 * of each key.
 *
 * @author Sanne Grinovero
 */
public class ConcurrentServiceBinding<K,V> {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Node EMPTY_LEAF = new Node( new Entry( 0, null, null ), null, null );

	@SuppressWarnings("unchecked")
	private volatile Node<K,V> treeRoot = EMPTY_LEAF;

	@SuppressWarnings("unchecked")
	public synchronized void clear() {
		treeRoot = EMPTY_LEAF;
	}

	public synchronized void put(final K key, final V value) {
		final int code = hashKey( key );
		final Entry<K,V> newEntry = new Entry<K, V>( code, key, value );
		final ArrayList<Entry<K, V>> list = convertToArrayList( treeRoot, key );
		list.add( newEntry );
		Collections.sort( list );
		final int size = list.size();
		@SuppressWarnings("unchecked")
		Entry<K, V>[] array = list.toArray( new Entry[size] );
		treeRoot = treeFromRange( array, 0, size );
	}

	private Node<K, V> treeFromRange(final Entry<K, V>[] array, final int minInclusive, final int maxExclusive) {
		if ( minInclusive == maxExclusive ) {
			return null;
		}
		//find the midpoint, rounding down to avoid the exclusion range:
		int mid = ( minInclusive + maxExclusive ) / 2;
		//shift to the right to make sure we won't have left children with the same hash:
		while ( mid > minInclusive && array[mid].hash == array[mid-1].hash ) {
			mid--;
		}
		return new Node( array[mid], treeFromRange( array, minInclusive, mid ), treeFromRange( array, mid + 1, maxExclusive ) );
	}

	public V get(final K key) {
		final int hash = hashKey( key );
		final Node<K,V> root = treeRoot;
		return root.get( key, hash );
	}

	protected int hashKey(final K key) {
		return System.identityHashCode( key );
	}

	public Iterable<V> values() {
		@SuppressWarnings("rawtypes")
		ArrayList<V> list = new ArrayList();
		treeRoot.collectAllValuesInto( list );
		return list;
	}

	private final ArrayList<Entry<K, V>> convertToArrayList(final Node<K, V> treeRoot, K exceptKey) {
		@SuppressWarnings("rawtypes")
		ArrayList<Entry<K, V>> list = new ArrayList();
		if ( treeRoot != EMPTY_LEAF ) {
			treeRoot.collectAllEntriesInto( list, exceptKey );
		}
		return list;
	}

	private static final class Entry<K,V> implements Comparable<Entry<K,V>> {

		private final int hash;
		private final K key;
		private final V value;

		Entry(int keyHashCode, K key, V value) {
			this.hash = keyHashCode;
			this.key = key;
			this.value = value;
		}

		@Override
		public int compareTo(Entry o) {
			//Sorting by the identity hashcode
			//Note: this class has a natural ordering that is inconsistent with equals.
			return ( hash < o.hash ) ? -1 : ( (hash == o.hash) ? 0 : 1 );
		}

		@Override
		public boolean equals(Object obj) {
			//A ClassCastException is really not expected here,
			//as it's an internal private class,
			//so just let it happen as a form of assertion.
			final Entry<K,V> other = (Entry<K,V>)obj;
			//Reference equality on the key only!
			return other.key == this.key;
		}

		@Override
		public String toString() {
			return "<" + key + ", " + value + ">";
		}
	}

	private static final class Node<K,V> {

		private final Entry<K,V> entry;
		private final Node<K, V> left;
		private final Node<K, V> right;

		Node(Entry<K,V> entry, Node<K,V> left, Node<K,V> right) {
			this.entry = entry;
			this.left = left;
			this.right = right;
		}

		public V get(final K key, final int hash) {
			if ( entry.key == key ) {
				return entry.value;
			}
			//Note that same-hashcode childs need to be on the right
			//as we don't test for equality, nor want to chase both
			//branches:
			else if ( hash < this.entry.hash ) {
				return left == null ? null : left.get( key, hash );
			}
			else {
				return right == null ? null : right.get( key, hash );
			}
		}

		public void collectAllEntriesInto(final List<Entry<K,V>> list, final K exceptKey) {
			if ( entry != null && exceptKey != entry.key ) {
				list.add( entry );
			}
			if ( left != null ) {
				left.collectAllEntriesInto( list, exceptKey );
			}
			if ( right != null ) {
				right.collectAllEntriesInto( list, exceptKey );
			}
		}

		public void collectAllValuesInto(final List<V> list) {
			if ( entry != null && entry.value != null ) {
				list.add( entry.value );
			}
			if ( left != null ) {
				left.collectAllValuesInto( list );
			}
			if ( right != null ) {
				right.collectAllValuesInto( list );
			}
		}

		/**
		 * Helper to visualize the tree via toString
		 */
		private void renderToStringBuilder(final StringBuilder sb, final int indent) {
			sb.append( entry );
			appendIndented( sb, indent, "L-> ", left );
			appendIndented( sb, indent, "R-> ", right );
		}

		private void appendIndented(final StringBuilder sb, final int indent, final String label, Node<K, V> node) {
			if ( node == null ) {
				return;
			}
			sb.append( "\n" );
			for ( int i = 0; i < indent; i++ ) {
				sb.append( "\t" );
			}
			sb.append( label );
			node.renderToStringBuilder( sb, indent + 1 );
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			renderToStringBuilder( sb, 0 );
			return sb.toString();
		}
	}

}
