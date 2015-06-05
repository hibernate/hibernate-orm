/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.service.ServiceRegistry;

/**
 * An interface for factories of {@link ProxyFactory proxy factory} instances.
 * <p/>
 * Currently used to abstract from the tupizer whether we are using CGLIB or
 * Javassist for lazy proxy generation.
 *
 * @author Steve Ebersole
 */
public interface ProxyFactoryFactory {
	/**
	 * Build a proxy factory specifically for handling runtime
	 * lazy loading.
	 *
	 * @return The lazy-load proxy factory.
	 */
	public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory);

	/**
	 * Build a proxy factory for basic proxy concerns.  The return
	 * should be capable of properly handling newInstance() calls.
	 * <p/>
	 * Should build basic proxies essentially equivalent to JDK proxies in
	 * terms of capabilities, but should be able to deal with abstract super
	 * classes in addition to proxy interfaces.
	 * <p/>
	 * Must pass in either superClass or interfaces (or both).
	 *
	 * @param superClass The abstract super class (or null if none).
	 * @param interfaces Interfaces to be proxied (or null if none).
	 * @return The proxy class
	 */
	public BasicProxyFactory buildBasicProxyFactory(Class superClass, Class[] interfaces);
}
