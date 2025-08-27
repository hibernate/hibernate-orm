/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy.map;

import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

/**
 * @author Gavin King
 */
public class MapProxyFactory implements ProxyFactory {

	private String entityName;

	public void postInstantiate(
			final String entityName,
			final Class<?> persistentClass,
			final Set<Class<?>> interfaces,
			final Method getIdentifierMethod,
			final Method setIdentifierMethod,
			CompositeType componentIdType) {
		this.entityName = entityName;
	}

	@Override
	public HibernateProxy getProxy(final Object id, final SharedSessionContractImplementor session) {
		return new MapProxy( new MapLazyInitializer( entityName, id, session ) );
	}

}
