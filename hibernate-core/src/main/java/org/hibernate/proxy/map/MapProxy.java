/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy.map;

import java.io.Serial;
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
@SuppressWarnings("rawtypes")
public class MapProxy implements HibernateProxy, Map, Serializable {

	private final MapLazyInitializer lazyInitializer;

	private Object replacement;

	MapProxy(MapLazyInitializer lazyInitializer) {
		this.lazyInitializer = lazyInitializer;
	}

	@Override
	public LazyInitializer getHibernateLazyInitializer() {
		return lazyInitializer;
	}

	@Override
	public int size() {
		return lazyInitializer.getMap().size();
	}

	@Override
	public void clear() {
		lazyInitializer.getMap().clear();
	}

	@Override
	public boolean isEmpty() {
		return lazyInitializer.getMap().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return lazyInitializer.getMap().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return lazyInitializer.getMap().containsValue(value);
	}

	@Override
	public Collection<?> values() {
		return lazyInitializer.getMap().values();
	}

	@Override @SuppressWarnings("unchecked")
	public void putAll(Map map) {
		lazyInitializer.getMap().putAll(map);
	}

	@Override
	public Set<?> entrySet() {
		return lazyInitializer.getMap().entrySet();
	}

	@Override
	public Set<?> keySet() {
		return lazyInitializer.getMap().keySet();
	}

	@Override
	public Object get(Object key) {
		return lazyInitializer.getMap().get(key);
	}

	@Override
	public Object remove(Object key) {
		return lazyInitializer.getMap().remove(key);
	}

	@Override @SuppressWarnings("unchecked")
	public Object put(Object key, Object value) {
		return lazyInitializer.getMap().put(key, value);
	}

	@Serial
	@Override
	public Object writeReplace() {
		/*
		 * If the target has already been loaded somewhere, just not set on the proxy,
		 * then use it to initialize the proxy so that we will serialize that instead of the proxy.
		 */
		lazyInitializer.initializeWithoutLoadIfPossible();

		if ( lazyInitializer.isUninitialized() ) {
			if ( replacement == null ) {
				lazyInitializer.prepareForPossibleLoadingOutsideTransaction();
				replacement = serializableProxy();
			}
			return replacement;
		}
		else {
			return lazyInitializer.getImplementation();
		}
	}

	private Object serializableProxy() {
		return new SerializableMapProxy(
				lazyInitializer.getEntityName(),
				lazyInitializer.getInternalIdentifier(),
				lazyInitializer.isReadOnlySettingAvailable()
						? Boolean.valueOf( lazyInitializer.isReadOnly() )
						: lazyInitializer.isReadOnlyBeforeAttachedToSession(),
				lazyInitializer.getSessionFactoryUuid(),
				lazyInitializer.getSessionFactoryName(),
				lazyInitializer.isAllowLoadOutsideTransaction()
		);
	}

}
