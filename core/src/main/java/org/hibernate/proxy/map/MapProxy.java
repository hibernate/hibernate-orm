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
 *
 */
package org.hibernate.proxy.map;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Proxy for "dynamic-map" entity representations.
 *
 * @author Gavin King
 */
public class MapProxy implements HibernateProxy, Map, Serializable {

	private MapLazyInitializer li;

	MapProxy(MapLazyInitializer li) {
		this.li = li;
	}

	public Object writeReplace() {
		return this;
	}

	public LazyInitializer getHibernateLazyInitializer() {
		return li;
	}

	public int size() {
		return li.getMap().size();
	}

	public void clear() {
		li.getMap().clear();
	}

	public boolean isEmpty() {
		return li.getMap().isEmpty();
	}

	public boolean containsKey(Object key) {
		return li.getMap().containsKey(key);
	}

	public boolean containsValue(Object value) {
		return li.getMap().containsValue(value);
	}

	public Collection values() {
		return li.getMap().values();
	}

	public void putAll(Map t) {
		li.getMap().putAll(t);
	}

	public Set entrySet() {
		return li.getMap().entrySet();
	}

	public Set keySet() {
		return li.getMap().keySet();
	}

	public Object get(Object key) {
		return li.getMap().get(key);
	}

	public Object remove(Object key) {
		return li.getMap().remove(key);
	}

	public Object put(Object key, Object value) {
		return li.getMap().put(key, value);
	}

}
