/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Clob;

import org.hibernate.HibernateException;

/**
 * Manages aspects of proxying {@link Clob Clobs} to add serializability.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class SerializableClobProxy implements InvocationHandler, Serializable {
	private static final Class[] PROXY_INTERFACES = new Class[] { Clob.class, WrappedClob.class, Serializable.class };

	private final transient Clob clob;

	/**
	 * Builds a serializable {@link java.sql.Clob} wrapper around the given {@link java.sql.Clob}.
	 *
	 * @param clob The {@link java.sql.Clob} to be wrapped.
	 * @see #generateProxy(java.sql.Clob)
	 */
	protected SerializableClobProxy(Clob clob) {
		this.clob = clob;
	}

	/**
	 * Access to the wrapped Clob reference
	 *
	 * @return The wrapped Clob reference
	 */
	public Clob getWrappedClob() {
		if ( clob == null ) {
			throw new IllegalStateException( "Clobs may not be accessed afterQuery serialization" );
		}
		else {
			return clob;
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( "getWrappedClob".equals( method.getName() ) ) {
			return getWrappedClob();
		}
		try {
			return method.invoke( getWrappedClob(), args );
		}
		catch ( AbstractMethodError e ) {
			throw new HibernateException( "The JDBC driver does not implement the method: " + method, e );
		}
		catch ( InvocationTargetException e ) {
			throw e.getTargetException();
		}
	}

	/**
	 * Generates a SerializableClobProxy proxy wrapping the provided Clob object.
	 *
	 * @param clob The Clob to wrap.
	 * @return The generated proxy.
	 */
	public static Clob generateProxy(Clob clob) {
		return (Clob) Proxy.newProxyInstance( getProxyClassLoader(), PROXY_INTERFACES, new SerializableClobProxy( clob ) );
	}

	/**
	 * Determines the appropriate class loader to which the generated proxy
	 * should be scoped.
	 *
	 * @return The class loader appropriate for proxy construction.
	 */
	public static ClassLoader getProxyClassLoader() {
		return WrappedClob.class.getClassLoader();
	}
}
