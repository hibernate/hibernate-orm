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
 */
package org.hibernate.envers.entities.mapper.relation.lazy.proxy;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.io.Serializable;

import org.hibernate.envers.entities.mapper.relation.lazy.initializor.Initializor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MapProxy<K, V> implements Map<K, V>, Serializable {
    private static final long serialVersionUID = 8418037541773074646L;

    private transient Initializor<Map<K, V>> initializor;
    protected Map<K, V> delegate;

    public MapProxy() {
    }

    public MapProxy(org.hibernate.envers.entities.mapper.relation.lazy.initializor.Initializor<Map<K, V>> initializor) {
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

    @Override
    public String toString() {
        checkInit();
        return delegate.toString();
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    @Override
    public boolean equals(Object obj) {
        checkInit();
        return delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        checkInit();
        return delegate.hashCode();
    }
}
