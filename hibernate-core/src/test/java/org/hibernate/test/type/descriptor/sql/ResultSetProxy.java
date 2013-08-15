/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.type.descriptor.sql;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Clob;
import java.sql.ResultSet;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class ResultSetProxy<T> implements InvocationHandler {
	public static ResultSet generateProxy(ResultSetProxy handler) {
		return ( ResultSet ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				new Class[] { ResultSet.class },
				handler
		);
	}

	private static ClassLoader getProxyClassLoader() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if ( cl == null ) {
			cl = ResultSet.class.getClassLoader();
		}
		return cl;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( method.getName().equals( methodName ) && args.length >= 1 ) {
			return value;
		}
		if ( method.getName().equals( "wasNull" ) ) {
			return value == null;
		}
		throw new UnsupportedOperationException( "Unexpected call ResultSet." + method.getName() );
	}

	private final String methodName;
	private final T value;

	protected ResultSetProxy(String methodName, T value) {
		this.methodName = methodName;
		this.value = value;
	}

	public static ResultSet generateProxy(final String value) {
		return generateProxy(
				new ResultSetProxy<String>( "getString", value )
		);
	}

	public static ResultSet generateProxy(final Clob value) {
		return generateProxy(
				new ResultSetProxy<Clob>( "getClob", value )
		);
	}
}
