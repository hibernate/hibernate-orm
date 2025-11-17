/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy;

import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.CompositeType;

/**
 * Contract for runtime, proxy-based lazy initialization proxies.
 *
 * @author Gavin King
 */
public interface ProxyFactory {

	/**
	 * Called immediately after instantiation of this factory.
	 * <p>
	 * Essentially equivalent to constructor injection, but contracted
	 * here via interface.
	 *
	 * @param entityName The name of the entity for which this factory should
	 * generate proxies.
	 * @param persistentClass The entity class for which to generate proxies;
	 * not always the same as the entityName.
	 * @param interfaces The interfaces to expose in the generated proxy;
	 * {@link HibernateProxy} is already included in this collection.
	 * @param getIdentifierMethod Reference to the identifier getter method;
	 * invocation on this method should not force initialization
	 * @param setIdentifierMethod Reference to the identifier setter method;
	 * invocation on this method should not force initialization
	 * @param componentIdType For composite identifier types, a reference to
	 * the {@linkplain CompositeType type} of the identifier
	 * property; again accessing the id should generally not cause
	 * initialization - but need to bear in mind &lt;key-many-to-one/&gt;
	 * mappings.
	 * @throws HibernateException Indicates a problem completing post
	 * instantiation initialization.
	 */
	void postInstantiate(
			String entityName,
			Class<?> persistentClass,
			Set<Class<?>> interfaces,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType) throws HibernateException;

	/**
	 * Create a new proxy instance
	 *
	 * @param id The id value for the proxy to be generated.
	 * @param session The session to which the generated proxy will be
	 * associated.
	 * @return The generated proxy.
	 * @throws HibernateException Indicates problems generating the requested
	 * proxy.
	 */
	HibernateProxy getProxy(Object id, SharedSessionContractImplementor session) throws HibernateException;
}
