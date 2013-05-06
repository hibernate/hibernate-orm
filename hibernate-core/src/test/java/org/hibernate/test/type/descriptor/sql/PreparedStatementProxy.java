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
import java.sql.PreparedStatement;
import java.sql.SQLException;

import junit.framework.Assert;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class PreparedStatementProxy<T> implements InvocationHandler {
	public static PreparedStatement generateProxy(PreparedStatementProxy handler) {
		return (PreparedStatement) Proxy.newProxyInstance(
				getProxyClassLoader(),
				new Class[] { PreparedStatement.class },
				handler
		);
	}

	private static ClassLoader getProxyClassLoader() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if ( cl == null ) {
			cl = PreparedStatement.class.getClassLoader();
		}
		return cl;
	}

	@SuppressWarnings({ "unchecked" })
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( value == null ) {
			Assert.assertEquals( "Expecting setNull call", "setNull", method.getName() );
			return null;
		}
		if ( method.getName().equals( methodName ) && args.length >= 1 ) {
			checkValue( (T) args[1] );
			return null;
		}
		throw new UnsupportedOperationException( "Unexpected call PreparedStatement." + method.getName() );
	}

	protected void checkValue(T arg) throws SQLException {
		Assert.assertEquals( value, arg );
	}

	protected final String extractString(Clob clob) throws SQLException {
		if ( StringClobImpl.class.isInstance( clob ) ) {
			return ( (StringClobImpl) clob ).getValue();
		}
		return clob.getSubString( 1, (int)clob.length() );
	}

	private final String methodName;
	private final T value;

	public T getValue() {
		return value;
	}

	protected PreparedStatementProxy(String methodName, T value) {
		this.methodName = methodName;
		this.value = value;
	}

	public static PreparedStatement generateProxy(final String value) {
		return generateProxy(
				new PreparedStatementProxy<String>( "setString", value )
		);
	}

	public static PreparedStatement generateProxy(Clob value) {
		return generateProxy(
				new PreparedStatementProxy<Clob>( "setClob", value ) {
					@Override
					protected void checkValue(Clob arg) throws SQLException {
						Assert.assertEquals( extractString( getValue() ), extractString( arg ) );
					}
				}
		);
	}
}
