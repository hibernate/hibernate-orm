/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.service.Service;

/**
 * An interface for factories of {@link ProxyFactory proxy factory} instances.
 * <p>
 * Currently used to abstract from the tuplizer whether we are using Byte Buddy or
 * possibly another implementation (in the future?) for lazy proxy generation.
 *
 * @author Steve Ebersole
 */
public interface ProxyFactoryFactory extends Service {
	/**
	 * Build a proxy factory specifically for handling runtime
	 * lazy loading.
	 *
	 * @return The lazy-load proxy factory.
	 */
	ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory);

	/**
	 * Build a proxy factory for basic proxy concerns.  The return
	 * should be capable of properly handling newInstance() calls.
	 * <p>
	 * Should build basic proxies essentially equivalent to JDK proxies in
	 * terms of capabilities, but should be able to deal with abstract super
	 * classes in addition to proxy interfaces.
	 * <p>
	 * Must pass in either a superClass or an interface.
	 *
	 * @param superClassOrInterface The abstract super class, or the
	 * interface to be proxied.
	 * @return The proxy class
	 */
	BasicProxyFactory buildBasicProxyFactory(Class superClassOrInterface);

}
