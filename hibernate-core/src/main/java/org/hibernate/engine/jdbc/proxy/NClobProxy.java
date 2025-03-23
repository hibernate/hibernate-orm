/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.proxy;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NClobImplementer;

import java.io.Reader;
import java.sql.NClob;

/**
 * Manages aspects of proxying {@link NClob}s for non-contextual creation, including proxy creation and
 * handling proxy invocations.  We use proxies here solely to avoid JDBC version incompatibilities.
 *
 * @apiNote This class is not intended to be called directly by the application program.
 *          Instead, use {@link org.hibernate.Session#getLobHelper()}.
 *
 * @see ClobProxy
 * @see BlobProxy
 * @see LobCreator
 * @see org.hibernate.LobHelper
 *
 * @author Steve Ebersole
 */
@Internal
public class NClobProxy extends ClobProxy implements NClob, NClobImplementer {

	protected NClobProxy(String string) {
		super( string );
	}

	protected NClobProxy(Reader reader, long length) {
		super( reader, length );
	}

	/**
	 * Generates a {@link java.sql.Clob} proxy using the string data.
	 *
	 * @param string The data to be wrapped as a {@link java.sql.Clob}.
	 *
	 * @return The generated proxy.
	 */
	public static NClob generateProxy(String string) {
		return new NClobProxy( string );
	}

	/**
	 * Generates a {@link NClob} proxy using a character reader of given length.
	 *
	 * @param reader The character reader
	 * @param length The length of the character reader
	 *
	 * @return The generated proxy.
	 */
	public static NClob generateProxy(Reader reader, long length) {
		return new NClobProxy( reader, length );
	}

	/**
	 * Determines the appropriate class loader to which the generated proxy
	 * should be scoped.
	 *
	 * @return The class loader appropriate for proxy construction.
	 */
	protected static ClassLoader getProxyClassLoader() {
		return NClobImplementer.class.getClassLoader();
	}
}
