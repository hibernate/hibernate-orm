// $Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.lob;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.sql.Blob;
import org.hibernate.HibernateException;

/**
 * A proxy for SerializableBlob objects which delegates all Blob methods to
 * a wrapped Blob.
 *
 * @author Gail Badner
 */
public class SerializableBlobProxy implements InvocationHandler {

	private static final Class[] PROXY_INTERFACES = new Class[] { SerializableBlob.class };

	private transient final Blob blob;

	/**
	 * Generates a SerializableBlob proxy wrapping the provided Blob object.
	 *
	 * @param blob The Blob to wrap.
	 * @return The generated proxy.
	 */
	public static Blob generateProxy(Blob blob) {
		return ( Blob ) Proxy.newProxyInstance(
				getProxyClassLoader(),
		        PROXY_INTERFACES,
		         new SerializableBlobProxy( blob )
		);
	}

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

	/**
	 * {@inheritDoc}
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( "getWrappedBlob".equals( method.getName() ) ) {
			return getWrappedBlob();
		}
		try {
			return method.invoke( getWrappedBlob(), args );
		}
		catch( AbstractMethodError e ) {
			throw new HibernateException("The JDBC driver does not implement the method: " + method, e);
		}
		catch (InvocationTargetException e ) {
			throw e.getTargetException();	
		}
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
			cl = SerializableBlob.class.getClassLoader();
		}
		return cl;
	}
}
