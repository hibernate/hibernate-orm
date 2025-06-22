/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import java.util.Hashtable;
import javax.naming.InitialContext;

/**
 * @author Steve Ebersole
 */
public interface EnvironmentSettings {
	/**
	 * Specifies the JNDI {@link javax.naming.spi.InitialContextFactory} implementation
	 * class to use.  Passed along to {@link InitialContext#InitialContext(Hashtable)}
	 * as {@value javax.naming.Context#INITIAL_CONTEXT_FACTORY}.
	 *
	 * @see javax.naming.Context#INITIAL_CONTEXT_FACTORY
	 */
	String JNDI_CLASS = "hibernate.jndi.class";

	/**
	 * Specifies the JNDI provider/connection URL.  Passed along to
	 * {@link InitialContext#InitialContext(Hashtable)} as
	 * {@value javax.naming.Context#PROVIDER_URL}.
	 *
	 * @see javax.naming.Context#PROVIDER_URL
	 */
	String JNDI_URL = "hibernate.jndi.url";

	/**
	 * A prefix for properties specifying arbitrary JNDI {@link javax.naming.InitialContext}
	 * properties. These properties are simply passed along to the constructor
	 * {@link javax.naming.InitialContext#InitialContext(java.util.Hashtable)}.
	 */
	String JNDI_PREFIX = "hibernate.jndi";

	/**
	 * Specifies a {@link java.util.Collection collection} of the {@link ClassLoader}
	 * instances Hibernate should use for classloading and resource loading.
	 *
	 * @since 5.0
	 */
	String CLASSLOADERS = "hibernate.classLoaders";

	/**
	 * Specifies how the {@linkplain Thread#getContextClassLoader() thread context}
	 * {@linkplain ClassLoader class loader} must be used for class lookup.
	 *
	 * @see org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence
	 */
	String TC_CLASSLOADER = "hibernate.classLoader.tccl_lookup_precedence";
}
