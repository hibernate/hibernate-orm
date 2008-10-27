/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation.lazy.proxy;

import org.jboss.envers.entities.mapper.relation.lazy.initializor.Initializor;

import java.util.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SortedMapProxy<K, V> implements SortedMap<K, V> {
    private Initializor<SortedMap<K, V>> initializor;
    protected SortedMap<K, V> delegate;

    public SortedMapProxy(org.jboss.envers.entities.mapper.relation.lazy.initializor.Initializor<SortedMap<K, V>> initializor) {
        this.initializor = initializor;
    }

    private void checkInit() {
        if (delegate == null) {
            delegate = initializor.initialize();
        }
    }

    public int size() {
        checkInit();
        return delegate.size();
    }

    public boolean isEmpty() {
        checkInit();
        return delegate.isEmpty();
    }

    public boolean containsKey(Object o) {
        checkInit();
        return delegate.containsKey(o);
    }

    public boolean containsValue(Object o) {
        checkInit();
        return delegate.containsValue(o);
    }

    public V get(Object o) {
        checkInit();
        return delegate.get(o);
    }

    public V put(K k, V v) {
        checkInit();
        return delegate.put(k, v);
    }

    public V remove(Object o) {
        checkInit();
        return delegate.remove(o);
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        checkInit();
        delegate.putAll(map);
    }

    public void clear() {
        checkInit();
        delegate.clear();
    }

    public Set<K> keySet() {
        checkInit();
        return delegate.keySet();
    }

    public Collection<V> values() {
        checkInit();
        return delegate.values();
    }

    public Set<Entry<K, V>> entrySet() {
        checkInit();
        return delegate.entrySet();
    }

    public Comparator<? super K> comparator() {
        checkInit();
        return delegate.comparator();
    }

    public SortedMap<K, V> subMap(K k, K k1) {
        checkInit();
        return delegate.subMap(k, k1);
    }

    public SortedMap<K, V> headMap(K k) {
        checkInit();
        return delegate.headMap(k);
    }

    public SortedMap<K, V> tailMap(K k) {
        checkInit();
        return delegate.tailMap(k);
    }

    public K firstKey() {
        checkInit();
        return delegate.firstKey();
    }

    public K lastKey() {
        checkInit();
        return delegate.lastKey();
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public boolean equals(Object o) {
        checkInit();
        return delegate.equals(o);
    }

    public int hashCode() {
        checkInit();
        return delegate.hashCode();
    }
}