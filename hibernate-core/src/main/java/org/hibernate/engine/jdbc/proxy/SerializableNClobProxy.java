/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.proxy;

import org.hibernate.Internal;

import java.lang.reflect.Proxy;
import java.sql.Clob;
import java.sql.NClob;

/**
 * Manages aspects of proxying {@link NClob}s to add serializability.
 *
 * @author Steve Ebersole
 */
@Internal
public class SerializableNClobProxy extends SerializableClobProxy {
	private static final Class<?>[] PROXY_INTERFACES = new Class[] { NClob.class, WrappedNClob.class };

	/**
	 * Builds a serializable {@link Clob} wrapper around the given {@link Clob}.
	 *
	 * @param clob The {@link Clob} to be wrapped.
	 *
	 * @see #generateProxy(Clob)
	 */
	protected SerializableNClobProxy(Clob clob) {
		super( clob );
	}

	/**
	 * Generates a SerializableNClobProxy proxy wrapping the provided NClob object.
	 *
	 * @param nclob The NClob to wrap.
	 * @return The generated proxy.
	 */
	public static NClob generateProxy(NClob nclob) {
		return (NClob) Proxy.newProxyInstance( getProxyClassLoader(), PROXY_INTERFACES, new SerializableNClobProxy( nclob ) );
	}

	/**
	 * Determines the appropriate class loader to which the generated proxy
	 * should be scoped.
	 *
	 * @return The class loader appropriate for proxy construction.
	 */
	public static ClassLoader getProxyClassLoader() {
		return SerializableClobProxy.getProxyClassLoader();
	}
}
