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
import java.sql.Blob;

import org.hibernate.HibernateException;

/**
 * Manages aspects of proxying {@link Blob Blobs} to add serializability.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class SerializableBlobProxy implements InvocationHandler, Serializable {
	private static final Class[] PROXY_INTERFACES = new Class[] { Blob.class, WrappedBlob.class, Serializable.class };

	private final transient Blob blob;

	/**
	 * Builds a serializable {@link Blob} wrapper around the given {@link Blob}.
	 *
	 * @param blob The {@link Blob} to be wrapped.
	 * @see
	 */
	private SerializableBlobProxy(Blob blob) {
		this.blob = blob;
	}

	/**
	 * Access to the wrapped Blob reference
	 *
	 * @return The wrapped Blob reference
	 */
	public Blob getWrappedBlob() {
		if ( blob == null ) {
			throw new IllegalStateException( "Blobs may not be accessed afterQuery serialization" );
		}
		else {
			return blob;
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( "getWrappedBlob".equals( method.getName() ) ) {
			return getWrappedBlob();
		}
		try {
			return method.invoke( getWrappedBlob(), args );
		}
		catch ( AbstractMethodError e ) {
			throw new HibernateException( "The JDBC driver does not implement the method: " + method, e );
		}
		catch ( InvocationTargetException e ) {
			throw e.getTargetException();
		}
	}

	/**
	 * Generates a SerializableBlob proxy wrapping the provided Blob object.
	 *
	 * @param blob The Blob to wrap.
	 *
	 * @return The generated proxy.
	 */
	public static Blob generateProxy(Blob blob) {
		return (Blob) Proxy.newProxyInstance( getProxyClassLoader(), PROXY_INTERFACES, new SerializableBlobProxy( blob ) );
	}

	/**
	 * Determines the appropriate class loader to which the generated proxy
	 * should be scoped.
	 *
	 * @return The class loader appropriate for proxy construction.
	 */
	public static ClassLoader getProxyClassLoader() {
		return WrappedBlob.class.getClassLoader();
	}
}
