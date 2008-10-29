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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Clob;

import org.hibernate.HibernateException;

/**
 * A proxy for SerializableClob objects which delegates all Clob methods to
 * a wrapped Clob.
 *
 * @author Gail Badner
 */
public class SerializableClobProxy implements InvocationHandler {

	private static final Class[] PROXY_INTERFACES = new Class[] { SerializableClob.class };

	private transient final Clob clob;

	/**
	 * Generates a SerializableClob proxy wrapping the provided Clob object.
	 *
	 * @param clob The Clob to wrap.
	 * @return The generated proxy.
	 */
	public static Clob generateProxy(Clob clob) {
		return ( Clob ) Proxy.newProxyInstance(
				getProxyClassLoader(),
		        PROXY_INTERFACES,
		         new SerializableClobProxy( clob )
		);
	}

	private SerializableClobProxy(Clob clob) {
		this.clob = clob;
	}

	public Clob getWrappedClob() {
		if ( clob==null ) {
			throw new IllegalStateException("Clobs may not be accessed after serialization");
		}
		else {
			return clob;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( "getWrappedClob".equals( method.getName() ) ) {
			return getWrappedClob();
		}
		try {
			return method.invoke( getWrappedClob(), args );
		}
		catch( AbstractMethodError e ) {
			throw new HibernateException("The JDBC driver does not implement the method: " + method, e);
		}
		catch( InvocationTargetException e ) {
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
			cl = Clob.class.getClassLoader();
		}
		return cl;
	}
}
