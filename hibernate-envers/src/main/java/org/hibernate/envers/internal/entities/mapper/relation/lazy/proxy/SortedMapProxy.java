/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SortedMapProxy<K, V> implements SortedMap<K, V>, Serializable {
	private static final long serialVersionUID = 2645817952901452375L;

	private transient Initializor<SortedMap<K, V>> initializor;
	protected SortedMap<K, V> delegate;

	public SortedMapProxy() {
	}

	public SortedMapProxy(org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor<SortedMap<K, V>> initializor) {
		this.initializor = initializor;
	}

	private void checkInit() {
		if ( delegate == null ) {
			delegate = initializor.initialize();
		}
	}

	@Override
	public int size() {
		checkInit();
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		checkInit();
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object o) {
		checkInit();
		return delegate.containsKey( o );
	}

	@Override
	public boolean containsValue(Object o) {
		checkInit();
		return delegate.containsValue( o );
	}

	@Override
	public V get(Object o) {
		checkInit();
		return delegate.get( o );
	}

	@Override
	public V put(K k, V v) {
		checkInit();
		return delegate.put( k, v );
	}

	@Override
	public V remove(Object o) {
		checkInit();
		return delegate.remove( o );
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		checkInit();
		delegate.putAll( map );
	}

	@Override
	public void clear() {
		checkInit();
		delegate.clear();
	}

	@Override
	public Set<K> keySet() {
		checkInit();
		return delegate.keySet();
	}

	@Override
	public Collection<V> values() {
		checkInit();
		return delegate.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		checkInit();
		return delegate.entrySet();
	}

	@Override
	public Comparator<? super K> comparator() {
		checkInit();
		return delegate.comparator();
	}

	@Override
	public SortedMap<K, V> subMap(K k, K k1) {
		checkInit();
		return delegate.subMap( k, k1 );
	}

	@Override
	public SortedMap<K, V> headMap(K k) {
		checkInit();
		return delegate.headMap( k );
	}

	@Override
	public SortedMap<K, V> tailMap(K k) {
		checkInit();
		return delegate.tailMap( k );
	}

	@Override
	public K firstKey() {
		checkInit();
		return delegate.firstKey();
	}

	@Override
	public K lastKey() {
		checkInit();
		return delegate.lastKey();
	}

	@Override
	@SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
	public boolean equals(Object o) {
		checkInit();
		return delegate.equals( o );
	}

	@Override
	public int hashCode() {
		checkInit();
		return delegate.hashCode();
	}
}
