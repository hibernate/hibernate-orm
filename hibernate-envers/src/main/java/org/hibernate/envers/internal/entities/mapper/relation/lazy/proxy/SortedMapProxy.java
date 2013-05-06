/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
