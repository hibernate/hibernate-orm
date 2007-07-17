//$Id: MapProxy.java 9210 2006-02-03 22:15:19Z steveebersole $
package org.hibernate.proxy.map;

import org.hibernate.proxy.map.MapLazyInitializer;
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
