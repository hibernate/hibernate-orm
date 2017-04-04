/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.lang.reflect.Proxy;
import java.sql.Clob;
import java.sql.NClob;

/**
 * Manages aspects of proxying java.sql.NClobs to add serializability.
 *
 * @author Steve Ebersole
 */
public class SerializableNClobProxy extends SerializableClobProxy {
	private static final Class[] PROXY_INTERFACES = new Class[] { NClob.class, WrappedNClob.class };

	/**
	 * Deprecated.
	 *
	 * @param clob The possible NClob reference
	 *
	 * @return {@code true} if the the Clob is a NClob as well
	 *
	 * @deprecated ORM baselines on JDK 1.6, so optional support for NClob (JDK 1,6 addition) is no longer needed.
	 */
	@Deprecated
	public static boolean isNClob(Clob clob) {
		return NClob.class.isInstance( clob );
	}

	/**
	 * Builds a serializable {@link java.sql.Clob} wrapper around the given {@link java.sql.Clob}.
	 *
	 * @param clob The {@link java.sql.Clob} to be wrapped.
	 *
	 * @see #generateProxy(java.sql.Clob)
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
