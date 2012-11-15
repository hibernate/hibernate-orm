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

	private transient final Blob blob;

	/**
	 * Builds a serializable {@link Blob} wrapper around the given {@link Blob}.
	 *
	 * @param blob The {@link Blob} to be wrapped.
	 * @see
	 */
	private SerializableBlobProxy(Blob blob) {
		this.blob = blob;
	}

	public Blob getWrappedBlob() {
		if ( blob == null ) {
			throw new IllegalStateException( "Blobs may not be accessed after serialization" );
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
		return ( Blob ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				PROXY_INTERFACES,
				new SerializableBlobProxy( blob )
		);
	}

	/**
	 * Determines the appropriate class loader to which the generated proxy
	 * should be scoped.
	 *
	 * @return The class loader appropriate for proxy construction.
	 */
	public static ClassLoader getProxyClassLoader() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if ( cl == null ) {
			cl = WrappedBlob.class.getClassLoader();
		}
		return cl;
	}
}
