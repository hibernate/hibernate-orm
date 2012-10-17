/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc;

import java.lang.reflect.Proxy;
import java.sql.Clob;

/**
 * Manages aspects of proxying java.sql.NClobs to add serializability.
 *
 * @author Steve Ebersole
 */
public class SerializableNClobProxy extends SerializableClobProxy {
	private static final Class NCLOB_CLASS = loadNClobClassIfAvailable();

	private static Class loadNClobClassIfAvailable() {
		try {
			return getProxyClassLoader().loadClass( "java.sql.NClob" );
		}
		catch ( ClassNotFoundException e ) {
			return null;
		}
	}

	private static final Class[] PROXY_INTERFACES = new Class[] { determineNClobInterface(), WrappedClob.class };

	private static Class determineNClobInterface() {
		// java.sql.NClob is a simple marker interface extending java.sql.Clob.  So if java.sql.NClob is not available
		// on the classloader, just use java.sql.Clob
		return NCLOB_CLASS == null ? Clob.class : NCLOB_CLASS;
	}

	public static boolean isNClob(Clob clob) {
		return NCLOB_CLASS != null && NCLOB_CLASS.isInstance( clob );
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
	 * Generates a SerializableClobProxy proxy wrapping the provided Clob object.
	 *
	 * @param clob The Clob to wrap.
	 * @return The generated proxy.
	 */
	public static Clob generateProxy(Clob clob) {
		return ( Clob ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				PROXY_INTERFACES,
				new SerializableNClobProxy( clob )
		);
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
