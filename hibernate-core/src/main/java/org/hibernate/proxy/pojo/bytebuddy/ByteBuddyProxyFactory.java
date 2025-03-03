/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy.pojo.bytebuddy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.PrimeAmongSecondarySupertypes;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyConfiguration;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_CLASS_ARRAY;

public class ByteBuddyProxyFactory implements ProxyFactory, Serializable {

	private final ByteBuddyProxyHelper byteBuddyProxyHelper;

	private Class<?> persistentClass;
	private String entityName;
	private Class<?>[] interfaces;
	private Method getIdentifierMethod;
	private Method setIdentifierMethod;
	private CompositeType componentIdType;
	private boolean overridesEquals;

	private Class<?> proxyClass;

	public ByteBuddyProxyFactory(ByteBuddyProxyHelper byteBuddyProxyHelper) {
		this.byteBuddyProxyHelper = byteBuddyProxyHelper;
	}

	@Override
	public void postInstantiate(
			String entityName,
			Class<?> persistentClass,
			Set<Class<?>> interfaces,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType) throws HibernateException {
		this.entityName = entityName;
		this.persistentClass = persistentClass;
		this.interfaces = toArray( interfaces );
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
		this.overridesEquals = ReflectHelper.overridesEquals( persistentClass );

		this.proxyClass = byteBuddyProxyHelper.buildProxy( persistentClass, this.interfaces );
	}

	private Class<?>[] toArray(Set<Class<?>> interfaces) {
		return interfaces == null ? EMPTY_CLASS_ARRAY : interfaces.toArray(EMPTY_CLASS_ARRAY);
	}

	@Override
	public HibernateProxy getProxy(
			Object id,
			SharedSessionContractImplementor session) throws HibernateException {
		final ByteBuddyInterceptor interceptor = new ByteBuddyInterceptor(
				entityName,
				persistentClass,
				interfaces,
				id,
				getIdentifierMethod,
				setIdentifierMethod,
				componentIdType,
				session,
				overridesEquals
		);

		final HibernateProxy instance = getHibernateProxy();
		final ProxyConfiguration proxyConfiguration = instance.asProxyConfiguration();
		if ( proxyConfiguration == null ) {
			throw new HibernateException( "Produced proxy does not correctly implement ProxyConfiguration" );
		}
		proxyConfiguration.$$_hibernate_set_interceptor( interceptor );
		return instance;
	}

	private HibernateProxy getHibernateProxy() {
		final PrimeAmongSecondarySupertypes internal = getHibernateProxyInternal();
		final HibernateProxy hibernateProxy = internal.asHibernateProxy();
		if ( hibernateProxy == null ) {
			throw new HibernateException( "Produced proxy does not correctly implement HibernateProxy" );
		}
		return hibernateProxy;
	}

	/**
	 * This technically returns a HibernateProxy, but declaring that type as the return
	 * type for the newInstance() action triggers an implicit case of type pollution.
	 * We therefore declare it as PrimeAmongSecondarySupertypes, and require the
	 * invoker to perform the narrowing
	 */
	private PrimeAmongSecondarySupertypes getHibernateProxyInternal() throws HibernateException {
		try {
			return (PrimeAmongSecondarySupertypes) proxyClass.getConstructor().newInstance();
		}
		catch (NoSuchMethodException e) {
			throw new HibernateException(
					"Bytecode enhancement failed because no public, protected or package-private default constructor was found for entity '"
							+ entityName + "' (private constructors don't work with runtime proxies)", e );
		}
		catch (Throwable t) {
			throw new HibernateException( "Bytecode enhancement failed for entity '" + entityName + "'", t );
		}
	}

}
