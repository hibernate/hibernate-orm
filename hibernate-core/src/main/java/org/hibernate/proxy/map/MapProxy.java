/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.map;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * Proxy for "dynamic-map" entity representations.
 *
 * @author Gavin King
 */
public class MapProxy implements HibernateProxy, Map, Serializable {

	private MapLazyInitializer li;

	private Object replacement;

	MapProxy(MapLazyInitializer li) {
		this.li = li;
	}

	@Override
	public LazyInitializer getHibernateLazyInitializer() {
		return li;
	}

	@Override
	public int size() {
		return li.getMap().size();
	}

	@Override
	public void clear() {
		li.getMap().clear();
	}

	@Override
	public boolean isEmpty() {
		return li.getMap().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return li.getMap().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return li.getMap().containsValue(value);
	}

	@Override
	public Collection values() {
		return li.getMap().values();
	}

	@Override
	public void putAll(Map t) {
		li.getMap().putAll(t);
	}

	@Override
	public Set entrySet() {
		return li.getMap().entrySet();
	}

	@Override
	public Set keySet() {
		return li.getMap().keySet();
	}

	@Override
	public Object get(Object key) {
		return li.getMap().get(key);
	}

	@Override
	public Object remove(Object key) {
		return li.getMap().remove(key);
	}

	@Override
	public Object put(Object key, Object value) {
		return li.getMap().put(key, value);
	}

	@Override
	public Object writeReplace() {
		/*
		 * If the target has already been loaded somewhere, just not set on the proxy,
		 * then use it to initialize the proxy so that we will serialize that instead of the proxy.
		 */
		li.initializeWithoutLoadIfPossible();

		if ( li.isUninitialized() ) {
			if ( replacement == null ) {
				li.prepareForPossibleLoadingOutsideTransaction();
				replacement = serializableProxy();
			}
			return replacement;
		}
		else {
			return li.getImplementation();
		}
	}

	private Object serializableProxy() {
		return new SerializableMapProxy(
				li.getEntityName(),
				li.getIdentifier(),
				( li.isReadOnlySettingAvailable() ? Boolean.valueOf( li.isReadOnly() ) : li.isReadOnlyBeforeAttachedToSession() ),
				li.getSessionFactoryUuid(),
				li.isAllowLoadOutsideTransaction()
		);
	}

}
